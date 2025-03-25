package br.dev.leandro.spring.cloud;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class) // Adicione esta linha
class GatewayRoutingTests {

    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        // Mock do endpoint do Keycloak
        stubFor(get(urlEqualTo("/auth/realms/myrealm/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));

        // Mock do seu serviço
        stubFor(get(urlEqualTo("/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"John Doe\"}")));
    }

    @Test
    void testUserServiceRouteWithAuth() {
        String token = "mocked-jwt-token"; // Ou gere um token real

        webClient.get()
                .uri("/api/users/1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe");
    }

    @Test
    void testUserServiceRoute() {
        // Configura o WireMock para simular o user-service
        stubFor(WireMock.get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"John Doe\"}")));

        webClient.get()
                .uri("/api/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe");
    }

}