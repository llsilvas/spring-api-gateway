package br.dev.leandro.spring.cloud.service;

import br.dev.leandro.spring.cloud.audit.service.AuditLogBodyService;
import br.dev.leandro.spring.cloud.jwt.JwtService;
import br.dev.leandro.spring.cloud.mock.JwtTokenMock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class AuditLogBodyServiceTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuditLogBodyService auditLogBodyService;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    @Test
    void testGetRequestBody_Successful() {
        // Arrange
        Mockito.when(exchange.getRequest()).thenReturn(request);

        Mockito.when(request.getHeaders()).thenReturn(headers);

        Mockito.when(headers.getFirst("Authorization")).thenReturn(getToken());

        Mockito.when(jwtService.parseToken(getToken())).thenReturn(JwtTokenMock.getJwtToken());

        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        DataBuffer buffer = dataBufferFactory.wrap(getRequestBody().getBytes(StandardCharsets.UTF_8));
        Flux<DataBuffer> bodyFlux = Flux.just(buffer);
        Mockito.when(request.getBody()).thenReturn(bodyFlux);

        Map<String, Object> attributes = new ConcurrentHashMap<>();
        Mockito.when(exchange.getAttributes()).thenReturn(attributes);

        // Act
        Mono<String> result = auditLogBodyService.getRequestBody(exchange);

        result.block();

        // Assert
        StepVerifier.create(result)
                .expectNext(getRequestBody())
                .verifyComplete();

        Assertions.assertEquals(JwtTokenMock.getJwtToken(), attributes.get(AuditLogBodyService.REQUEST_JWT_ATTRIBUTE));
        Assertions.assertEquals(getRequestBody(), attributes.get(AuditLogBodyService.REQUEST_BODY_ATTRIBUTE));

        Mockito.verify(exchange).getRequest();
        Mockito.verify(request).getHeaders();
        Mockito.verify(headers).getFirst("Authorization");
        Mockito.verify(jwtService).parseToken(getToken());
        Mockito.verify(request).getBody();
    }

    @NotNull
    private static String getToken() {
        return "Bearer sample.jwt.token";
    }

    @NotNull
    private static String getRequestBody() {
        return "{\"name\":\"John Doe\",\"age\":30}";
    }
}

