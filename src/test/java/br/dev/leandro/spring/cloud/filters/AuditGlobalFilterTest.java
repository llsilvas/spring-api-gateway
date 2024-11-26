package br.dev.leandro.spring.cloud.filters;

import br.dev.leandro.spring.cloud.audit.service.AuditLogBodyService;
import br.dev.leandro.spring.cloud.jwt.JwtToken;
import br.dev.leandro.spring.cloud.mock.JwtTokenMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditGlobalFilterTest {

    @Mock
    private AuditLogBodyService auditLogBodyService;

    @InjectMocks
    private AuditGlobalFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange decoratedExchange;


    @Test
    public void testAuditGlobalFilter() {
        // Arrange

        when(exchange.getAttributes()).thenReturn(new HashMap<>() {{
            put("REQUEST_JWT_TOKEN", JwtTokenMock.getJwtToken());
        }});

        String requestBody = "{\"name\":\"John Doe\",\"age\":30}";

        when(auditLogBodyService.getRequestBody(exchange)).thenReturn(Mono.just(requestBody));
        when(auditLogBodyService.getResponseBody(
                eq(exchange),
                eq(requestBody),
                anyString(),
                eq(JwtTokenMock.getJwtToken())))
                .thenReturn(Mono.just(decoratedExchange));

        when(auditLogBodyService.getResponseBody(
                eq(exchange),
                eq(null),
                anyString(),
                eq(JwtTokenMock.getJwtToken())))
                .thenReturn(Mono.just(decoratedExchange));
        when(chain.filter(decoratedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);
        result.block(); // Executa a cadeia reativa

        // Assert
        verify(auditLogBodyService, times(1)).getRequestBody(exchange);
        verify(auditLogBodyService, times(1)).getResponseBody(
                eq(exchange),
                eq(requestBody),
                anyString(),
                eq(JwtTokenMock.getJwtToken())
        );
        verify(chain, times(2)).filter(decoratedExchange);
    }
}
