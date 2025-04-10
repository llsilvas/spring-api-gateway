package br.dev.leandro.spring.cloud.controller;

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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/handle")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(ServerWebExchange exchange) {
        Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        HttpStatus status;
        String message;

        if (exception instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Circuit breaker está aberto para o serviço de usuários.";
        } else if (exception instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Timeout ao tentar acessar o serviço de usuários.";
        } else if (exception instanceof RequestNotPermitted) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            message = "Limite de requisições atingido para o serviço de usuários.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Erro desconhecido ao acessar o serviço de usuários.";
        }

        log.warn("Fallback acionado - [{}] {}", status.value(), message, exception);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(status).body(response));
    }
}
