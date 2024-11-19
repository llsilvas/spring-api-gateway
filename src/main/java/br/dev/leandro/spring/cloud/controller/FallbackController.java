package br.dev.leandro.spring.cloud.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
public class FallbackController {

    @RequestMapping("/fallback/user")
    public Mono<String> userServiceFallback(ServerWebExchange exchange) {
        Throwable throwable = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        if (throwable != null) {
            log.error("Fallback triggered due to: {}", throwable.getMessage());
        }

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        // Não modifique os cabeçalhos aqui
        // Retorna uma resposta simples
        return Mono.just("O serviço está temporariamente indisponível. Tente novamente mais tarde.");
    }
}
