package br.dev.leandro.spring.cloud.filters;

import br.dev.leandro.spring.cloud.audit.service.AuditLogBodyService;
import br.dev.leandro.spring.cloud.jwt.JwtToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class AuditGlobalFilter implements GlobalFilter, Ordered {
    private static final String REQUEST_JWT_ATTRIBUTE = "REQUEST_JWT_TOKEN";
    private final AuditLogBodyService auditLogBodyService;

    public AuditGlobalFilter(AuditLogBodyService auditLogBodyService) {
        this.auditLogBodyService = auditLogBodyService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting AuditGlobalFilter for CorrelationId: {}", correlationId);

        return auditLogBodyService.getRequestBody(exchange)
                .flatMap(requestBody -> {
                    log.info("Captured Request Body: {}", requestBody);
                    return auditLogBodyService.getResponseBody(exchange, requestBody, correlationId,
                                    (JwtToken) exchange.getAttributes().get(REQUEST_JWT_ATTRIBUTE))
                            .flatMap(chain::filter);
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.info("Proceeding with decorated exchange (without body)");
                            return auditLogBodyService.getResponseBody(exchange, null, correlationId, (JwtToken) exchange.getAttributes().get(REQUEST_JWT_ATTRIBUTE)).flatMap(chain::filter);
                        })
                )
                .doOnError(error -> log.error("Error in AuditGlobalFilter: {}", error.getMessage(), error));
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
