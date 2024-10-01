package br.dev.leandro.spring.cloud;

import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log
@SpringBootApplication
public class SpringApiGatewayApplication {

    public static void main(String[] args) {
        log.info(":: Iniciando Spring-Api-Gateway ::");
        long startTime = System.currentTimeMillis(); // Captura o tempo de início
        SpringApplication.run(SpringApiGatewayApplication.class, args);

        long endTime = System.currentTimeMillis(); // Captura o tempo de fim
        long totalTime = endTime - startTime; // Calcula o tempo total em milissegundos
        log.info(":: Spring-Api-Gateway iniciado com sucesso :: - " + totalTime + " ms" );
    }

}
