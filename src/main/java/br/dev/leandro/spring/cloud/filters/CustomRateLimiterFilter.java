package br.dev.leandro.spring.cloud.filters;

import br.dev.leandro.spring.cloud.dto.ErrorResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component("CustomRateLimiterFilter")
public class CustomRateLimiterFilter extends AbstractGatewayFilterFactory<CustomRateLimiterFilter.Config> {


    public CustomRateLimiterFilter() {
        super(Config.class);
    }


    @Getter
    @Setter
    public static class Config {
        private String routeId;
        private int limit = 5;          // padrão
        private long windowMs = 10000;  // padrão 10s
    }

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper mapper;


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String ip = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                    .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(addr -> addr.getAddress().getHostAddress())
                            .orElse("unknown"));


            String redisKey = String.format("rate_limit:%s:%s", config.getRouteId(), ip);
            int limit = config.getLimit();

            log.debug("[RateLimiter] Executando | IP={} | Rota={} | Limite={} | Janela={}ms", ip, config.getRouteId(), limit, config.getWindowMs());

            return redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMillis(config.getWindowMs()))
                    .flatMap(isNew -> {
                        log.debug("[RateLimiter] Chave Redis criada? {} | Chave={}", isNew, redisKey);
                        return Boolean.TRUE.equals(isNew)
                                ? Mono.just(1L)
                                : redisTemplate.opsForValue().increment(redisKey);
                    })
                    .flatMap(count -> {
                        ServerHttpResponse response = exchange.getResponse();
                        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
                        if (count > limit) {

                            log.warn("[RateLimiter] Excedido | IP={} | Rota={} | {}/{}", ip, config.getRouteId(), count, limit);

                            response.getHeaders().add("Retry-After", String.valueOf(Duration.ofMillis(config.getWindowMs()).toSeconds()));

                            ErrorResponseDto body = new ErrorResponseDto(
                                    429,
                                    "Muitas Requisições",
                                    "Limite de requisições excedido para o IP " + ip + ". Aguarde antes de tentar novamente.",
                                    exchange.getRequest().getPath().toString(),
                                    Instant.now(),
                                    null
                            );
                            return writeJsonResponse(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS, body);

                        } else if (count == 1) {
                            log.info("[RateLimiter] Nova janela | IP={} | Rota={} | Limite={} | Janela={}ms", ip, config.getRouteId(), limit, config.getWindowMs());
                        } else if (count == limit) {
                            log.info("[RateLimiter] Última requisição antes do limite | IP={} | Rota={} | {}/{}", ip, config.getRouteId(), count, limit);
                        } else {
                            log.debug("[RateLimiter] Permitida | IP={} | Rota={} | Requisições={}/{}", ip, config.getRouteId(), count, limit);
                        }

                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Void> writeJsonResponse(ServerHttpResponse response, HttpStatus status, Object body) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            response.setStatusCode(status);
            response.getHeaders().add("Content-Type", "application/json");
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

}
