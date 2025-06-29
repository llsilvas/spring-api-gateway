package br.dev.leandro.spring.cloud.controller;

import br.dev.leandro.spring.cloud.dto.ErrorResponseDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/handle")
    public Mono<ResponseEntity<ErrorResponseDto>> fallback(ServerWebExchange exchange) {
        Throwable raw = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        Throwable exception = unwrap(raw);
        Set<URI> originalUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);

        String requestUri = (originalUri != null && !originalUri.isEmpty())
                ? originalUri.iterator().next().getPath()
                : exchange.getRequest().getURI().getPath();


        ErrorMeta meta = resolveHttpStatusAndMessage(exception);
        log.warn("Fallback acionado [{}] - URI: {}, Excecao: {}", meta.status.value(), requestUri,
                exception != null ? exception.getClass().getSimpleName() : "n/a", exception);

        ErrorResponseDto response = new ErrorResponseDto(
                meta.status.value(),
                meta.status.getReasonPhrase(),
                meta.message,
                requestUri,
                Instant.now(),
                (exception != null) ? exception.getClass().getSimpleName() : null
        );

        return Mono.just(ResponseEntity.status(meta.status).body(response));
    }

    private Throwable unwrap(Throwable exception) {
        while (exception != null && exception.getCause() != null && exception != exception.getCause()) {
            exception = exception.getCause();
        }
        return exception;
    }

    private ErrorMeta resolveHttpStatusAndMessage(Throwable ex) {
        return switch (ex) {
            case CallNotPermittedException callNotPermittedException ->
                    new ErrorMeta(HttpStatus.SERVICE_UNAVAILABLE, "Circuit breaker ativado. Serviço indisponível.");
            case ConnectException connectException ->
                    new ErrorMeta(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao serviço.");
            case TimeoutException timeoutException ->
                    new ErrorMeta(HttpStatus.GATEWAY_TIMEOUT, "Tempo limite excedido ao chamar o serviço.");
            case RequestNotPermitted requestNotPermitted ->
                    new ErrorMeta(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisições atingido. Tente novamente mais tarde.");
            case null, default ->
                    new ErrorMeta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao acessar o serviço.");
        };
    }

    private record ErrorMeta(HttpStatus status, String message) {}

}
