package br.dev.leandro.spring.cloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user")
    public Mono<ResponseEntity<String>> userServiceFallback() {
        log.info("Fallback para user-service acionado");
        var message = "O serviço de usuário está temporariamente indisponível. Por favor, tente novamente mais tarde.";
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(message));
    }

}
