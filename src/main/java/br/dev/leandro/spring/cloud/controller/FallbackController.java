package br.dev.leandro.spring.cloud.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/user")
    public Mono<ResponseEntity<String>> userServiceFallback() {
        var message = "O serviço de usuário está temporariamente indisponível. Por favor, tente novamente mais tarde.";
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(message));
    }
}
