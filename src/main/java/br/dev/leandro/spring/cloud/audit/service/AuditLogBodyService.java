package br.dev.leandro.spring.cloud.audit.service;

import br.dev.leandro.spring.cloud.audit.dto.AuditLogDTO;
import br.dev.leandro.spring.cloud.audit.dto.RequestDTO;
import br.dev.leandro.spring.cloud.audit.dto.ResponseDTO;
import br.dev.leandro.spring.cloud.jwt.JwtService;
import br.dev.leandro.spring.cloud.jwt.JwtToken;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AuditLogBodyService {

    public static final String REQUEST_BODY_ATTRIBUTE = "REQUEST_BODY";
    public static final String REQUEST_JWT_ATTRIBUTE = "REQUEST_JWT_TOKEN";

    private final AuditLogService auditLogService;
    private final JwtService jwtService;

    public AuditLogBodyService(AuditLogService auditLogService, JwtService jwtService) {
        this.auditLogService = auditLogService;
        this.jwtService = jwtService;
    }

    public Mono<String> getRequestBody(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        String authHeader = request.getHeaders().getFirst("Authorization");
        JwtToken jwtToken = jwtService.parseToken(authHeader);
        exchange.getAttributes().put(REQUEST_JWT_ATTRIBUTE, jwtToken);

        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    // Libera o buffer para evitar vazamento de memória
                    DataBufferUtils.release(dataBuffer);
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    // Armazena o corpo nos atributos do exchange
                    exchange.getAttributes().put(REQUEST_BODY_ATTRIBUTE, body);
                    log.info("Request Body: {}", body);
                    return body;
                });

    }

    public ServerWebExchange decorateExchangeWithBody(ServerWebExchange exchange) {
        String body = exchange.getAttributeOrDefault(REQUEST_BODY_ATTRIBUTE, "");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return Mono.just(buffer);
        });

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @NotNull
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                return httpHeaders;
            }

            @NotNull
            @Override
            public Flux<DataBuffer> getBody() {
                return cachedFlux;
            }
        };

        return exchange.mutate().request(decoratedRequest).build();
    }

    @NotNull
    public Mono<ServerWebExchange> getResponseBody(ServerWebExchange exchange, String requestBody, String correlationId, JwtToken jwtToken) {
        log.info("Captured Request Body: {}", requestBody);
        ServerWebExchange serverWebExchange = decorateExchangeWithBody(exchange);

        ServerHttpRequest mutatedRequest = addCorrelationId(serverWebExchange, correlationId);
        ServerWebExchange mutatedExchange = serverWebExchange.mutate()
                .request(mutatedRequest)
                .build();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(mutatedExchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    return super.writeWith(
                            fluxBody.buffer().flatMap(dataBuffers -> {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                dataBuffers.forEach(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    try {
                                        baos.write(bytes);
                                    } catch (IOException e) {
                                        log.error("Error writing response body", e);
                                    }
                                    DataBufferUtils.release(dataBuffer);
                                });
                                String responseBody = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                                log.info("Captured Response Body: {}", responseBody);
                                AuditLogDTO auditLogDTO = buildAuditLogDTO(mutatedExchange, correlationId, requestBody, responseBody, jwtToken);

                                return auditLogService.salvarLog(auditLogDTO)
                                        .then(Mono.just(mutatedExchange.getResponse().bufferFactory().wrap(baos.toByteArray())));
                            })
                    );
                }
                return super.writeWith(body);
            }
        };
        return Mono.just(mutatedExchange.mutate().response(decoratedResponse).build());
//        return mutatedExchange.mutate().response(decoratedResponse).build();
    }

    @NotNull
    private AuditLogDTO buildAuditLogDTO(ServerWebExchange serverWebExchange, String correlationId, String requestBody, String responseBody, JwtToken jwtToken) {
        return new AuditLogDTO(
                correlationId,
                serverWebExchange.getRequest().getURI().getPath(),
                new RequestDTO(
                        serverWebExchange.getRequest().getMethod().name(),
                        serverWebExchange.getRequest().getHeaders().getFirst("X-Forwarded-For"),
                        serverWebExchange.getRequest().getHeaders().getFirst("User-Agent"),
                        getHeaders(serverWebExchange.getRequest().getHeaders()),
                        serverWebExchange.getRequest().getQueryParams().toSingleValueMap(),
                        requestBody),
                new ResponseDTO(
                        serverWebExchange.getResponse().getStatusCode() != null ? serverWebExchange.getResponse().getStatusCode().value() : 500,
                        getHeaders(serverWebExchange.getResponse().getHeaders()),
                        responseBody),
                jwtToken
        );
    }

    @NotNull
    private Map<String, String> getHeaders(HttpHeaders headers) {

        return headers.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            if("Authorization".equalsIgnoreCase(entry.getKey())) {
                                return maskAuthorizationHeader(entry.getValue());
                            }
                            return String.join(",", entry.getValue());
                        }
                ));
    }

    private String maskAuthorizationHeader(List<String> values) {
        return values.stream()
                .map(value -> {
                    if (value.startsWith("Bearer ")) {
                        return "Bearer ***";
                    }
                    return "***";
                })
                .collect(Collectors.joining(","));
    }

    public ServerHttpRequest addCorrelationId(ServerWebExchange exchange, String correlationId) {
        // Criar novos headers e copiar os existentes
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.putAll(exchange.getRequest().getHeaders());
        newHeaders.add("X-Correlation-Id", correlationId);

        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return newHeaders;
            }
        };
    }
}
