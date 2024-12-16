package br.dev.leandro.spring.cloud.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class AuditGlobalFilter implements GlobalFilter, Ordered {

    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;

    @Autowired
    public AuditGlobalFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter) {
        this.modifyResponseBodyFilter = modifyResponseBodyFilter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var correlationId = UUID.randomUUID().toString();

        Instant startTime = Instant.now();
        logRequest(exchange, correlationId);

        ServerHttpRequest mutatedRequest = getMutatedRequest(exchange, correlationId);
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        ModifyResponseBodyGatewayFilterFactory.Config config = logResponse(correlationId, startTime);
        return modifyResponseBodyFilter.apply(config).filter(mutatedExchange, chain);
    }

    private static ServerHttpRequest getMutatedRequest(ServerWebExchange exchange, String correlationId) {
        // Criar novos headers e copiar os existentes
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.putAll(exchange.getRequest().getHeaders());
        newHeaders.add("X-Correlation-Id", correlationId);

        // Criar um novo ServerHttpRequest com os novos headers
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return newHeaders;
            }
        };
    }

    private static ModifyResponseBodyGatewayFilterFactory.Config logResponse(String correlationId, Instant startTime) {
        // Configurar o filtro para modificar (ou apenas ler) o corpo da resposta
        ModifyResponseBodyGatewayFilterFactory.Config config = new ModifyResponseBodyGatewayFilterFactory.Config();
        config.setRewriteFunction(String.class, String.class, (serverWebExchange, originalBody) -> {
            // Logar o corpo da resposta
            int statusCode = serverWebExchange.getResponse().getStatusCode() != null ? serverWebExchange.getResponse().getStatusCode().value() : 500;

            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
            log.info("Response sent: correlationId={}, statusCode={}, body={}, durationMs={}",
                    correlationId, statusCode, originalBody, duration);

            // Retornar o corpo original sem modificações
            return Mono.just(originalBody);
        });
        return config;
    }

    private void logRequest(ServerWebExchange exchange, String correlationId) {
        // Extrair as informações da requisição
        String method = exchange.getRequest().getMethod().name();
        String uri = exchange.getRequest().getURI().toString();
        String queryParams = exchange.getRequest().getQueryParams().toString();
        String clientIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null && exchange.getRequest().getRemoteAddress() != null) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String userId = extractUserId(exchange);

        // Registrar as informações no log
        log.info("Request received: correlationId={}, method={}, uri={}, queryParams={}, clientIp={}, userAgent={}, userId={}",
                correlationId, method, uri, queryParams, clientIp, userAgent, userId);
    }

    private String extractUserId(ServerWebExchange exchange) {
        // Implementar a extração do ID do usuário autenticado, se aplicável
        // Por exemplo, a partir de um header ou do token JWT
        return exchange.getRequest().getHeaders().getFirst("X-User-Id");
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
