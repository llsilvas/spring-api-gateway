package br.dev.leandro.spring.cloud;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CircuitBreakerTests {

    private static final String USER_API_PATH = "/api/users/1";
    private static final String FALLBACK_RESPONSE_PHRASE = "indisponível";
    private static final String CIRCUIT_BREAKER_NAME = "userServiceCB";

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME).reset();
    }

    private void stubUserApiResponse(int status, String body, int delay) {
        stubFor(get(urlEqualTo(USER_API_PATH)).willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body).withFixedDelay(delay)));
    }

    private void stubSuccessfulUserApiResponse() {
        stubUserApiResponse(200, "{\"id\":1,\"name\":\"John Doe\"}", 100);
    }

    private void stubSlowUserApiResponse() {
        stubUserApiResponse(503, "{\"id\":1,\"name\":\"Slow Response\"}", 2000);
    }

    private void stubFailedUserApiResponse() {
        stubUserApiResponse(503, "Service Unavailable", 0);
    }

    private void makeApiCalls(int times, int expectedStatus) {
        IntStream.range(0, times).forEach(i -> {
            webClient.get().uri(USER_API_PATH).exchange().expectStatus().isEqualTo(expectedStatus);
        });
    }

    @Nested
    class WhenServiceIsHealthy {
        @BeforeEach
        void setUp() {
            stubSuccessfulUserApiResponse();
        }

        @Test
        void shouldReturnUserSuccessfully() {
            webClient.get().uri(USER_API_PATH).exchange().expectStatus().isOk().expectBody().jsonPath("$.id").isEqualTo(1).jsonPath("$.name").isEqualTo("John Doe");
        }

        @Test
        void circuitBreakerShouldRemainClosed() {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    class WhenServiceIsSlow {

        @BeforeEach
        void setUp() {
            stubSlowUserApiResponse();
        }

        @Test
        void shouldOpenCircuitBreakerWhenSlowCallThresholdExceeded() {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

            makeApiCalls(5, 503);

            // 1. Verifica estado inicial
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSlowCalls()).isEqualTo(3);
        }
    }

    @Nested
    class WhenServiceFails {
        @BeforeEach
        void setUp() {
            stubFailedUserApiResponse();
        }

        @Test
        void shouldOpenCircuitBreakerAfterFailures() {
            // Estado inicial
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // Chamadas para abrir o circuit breaker
            makeApiCalls(5, 503);

            // Verifica se abriu
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Verifica resposta de fallback
            webClient.get().uri(USER_API_PATH).exchange().expectStatus().isEqualTo(503).expectBody(String.class).value(response -> assertThat(response).contains(FALLBACK_RESPONSE_PHRASE));
        }


        @Test
        void shouldReturnConsistentErrorFormat1() {
            webClient.get().uri(USER_API_PATH).exchange().expectStatus().isEqualTo(503).expectHeader().contentType("text/plain;charset=UTF-8").expectBody(String.class).value(response -> {

//                        assertThat(response).contains("Service Unavailable");
                assertThat(response).contains(FALLBACK_RESPONSE_PHRASE);
            });

        }

//        @Test
//        void shouldReturnConsistentErrorFormat() {
//            webClient.get()
//                    .uri(USER_API_PATH)
//                    .exchange()
//                    .expectStatus().isEqualTo(503)
//                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                    .expectBody()
//                    .jsonPath("$.timestamp").exists()
//                    .jsonPath("$.status").isEqualTo(503)
//                    .jsonPath("$.error").isEqualTo("Service Unavailable")
//                    .jsonPath("$.message").value(message ->
//                            assertThat(message).asString().contains(FALLBACK_RESPONSE_PHRASE));
//        }
    }

    @Test
    void shouldRecoverAfterFailure() {
        // Fase 1: Falhas para abrir o circuit breaker
        stubFailedUserApiResponse();
        makeApiCalls(5, 503);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Fase 2: Espera e configura sucesso
        await().atMost(5, SECONDS).until(() -> {
            stubSuccessfulUserApiResponse();
            return circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN;
        });

        // Fase 3: Verifica meio-aberto
        webClient.get().uri(USER_API_PATH).exchange().expectStatus().isOk();

        // Fase 4: Verifica se fechou após sucesso
        await().atMost(1, SECONDS).until(() -> circuitBreaker.getState() == CircuitBreaker.State.CLOSED);
    }
}