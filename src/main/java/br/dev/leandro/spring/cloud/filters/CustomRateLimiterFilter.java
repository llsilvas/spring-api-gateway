package br.dev.leandro.spring.cloud.filters;

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
                    .orElseGet(() -> exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());

            String redisKey = String.format("rate_limit:%s:%s", config.getRouteId(), ip);
            int limit = config.getLimit();

            return redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMillis(config.getWindowMs()))
                    .flatMap(isNew -> Boolean.TRUE.equals(isNew)
                            ? Mono.just(1L)
                            : redisTemplate.opsForValue().increment(redisKey)
                    )
                    .flatMap(count -> {
                        if (count > limit) {
                            log.warn(":: Rate limit EXCEDIDO | IP: {} | Rota: {} | Requisições: {} | Limite: {}",
                                    ip, config.getRouteId(), count, limit);

                            Map<String, Object> body = Map.of(
                                    "status", 429,
                                    "error", "Too Many Requests",
                                    "message", "Rate limit exceeded for IP " + ip,
                                    "timestamp", Instant.now().toString()
                            );

                            byte[] bytes;
                            try {
                                bytes = mapper.writeValueAsBytes(body);
                            } catch (JsonProcessingException e) {
                                return Mono.error(e);
                            }

                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            response.getHeaders().add("Content-Type", "application/json");

                            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
                        } else if (count == 1) {
                            log.info(":: Iniciando nova janela de rate limit | IP: {} | Rota: {} | Limite: {} | Janela: {}ms",
                                    ip, config.getRouteId(), limit, config.getWindowMs());
                        } else if (count == limit) {
                            log.info(":: Última requisição antes de exceder o limite | IP: {} | Rota: {} | Requisições: {}/{}",
                                    ip, config.getRouteId(), count, limit);
                        }

                        return chain.filter(exchange);
                    });
        };
    }

}
