# Spring API Gateway

## Descrição
Projeto de estudo implementando um API Gateway utilizando Spring Boot 3.3.* e Spring Cloud Gateway. Este gateway serve como ponto de entrada centralizado para microserviços, oferecendo recursos avançados como roteamento, balanceamento de carga, segurança, limitação de taxa e resiliência.

## Tecnologias Utilizadas

### Principais Tecnologias
- **Java 21**: Versão mais recente do Java com recursos avançados
- **Spring Boot 3.4.5**: Framework para criação de aplicações Java com configuração simplificada
- **Spring Cloud Gateway**: Implementação de API Gateway baseada em Spring WebFlux
- **Spring WebFlux**: Framework reativo para aplicações web não bloqueantes
- **Spring Security**: Framework para autenticação e autorização
- **Spring Cloud Config**: Gerenciamento centralizado de configuração (desativado por padrão)
- **Spring Cloud Circuit Breaker**: Implementação do padrão Circuit Breaker para resiliência
- **Redis**: Armazenamento em memória para limitação de taxa e cache
- **RabbitMQ**: Message broker para comunicação assíncrona entre serviços

### Segurança
- **OAuth2 Resource Server**: Autenticação baseada em tokens JWT
- **Keycloak**: Servidor de identidade para gerenciamento de usuários e autenticação

### Observabilidade
- **Micrometer**: Biblioteca para métricas de aplicação
- **Prometheus**: Coleta e armazenamento de métricas
- **OpenTelemetry**: Instrumentação para rastreamento distribuído
- **Loki**: Agregação de logs

### Ferramentas de Desenvolvimento
- **Lombok**: Redução de código boilerplate
- **Spring Boot DevTools**: Ferramentas para desenvolvimento rápido
- **Docker/JKube**: Containerização e implantação em Kubernetes

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── br/dev/leandro/spring/cloud/
│   │       ├── config/                  # Configurações da aplicação
│   │       │   ├── RateLimiterConfig.java  # Configuração de limitação de taxa
│   │       │   ├── SecurityConfig.java     # Configuração de segurança
│   │       │   └── converter/              # Conversores personalizados
│   │       ├── controller/              # Controladores REST
│   │       │   └── FallbackController.java # Controlador para fallbacks
│   │       ├── exception/               # Exceções personalizadas
│   │       │   └── RateLimitException.java # Exceção para limite de taxa
│   │       ├── filters/                 # Filtros do Gateway
│   │       │   ├── AuditGlobalFilter.java       # Filtro para auditoria
│   │       │   └── CustomRateLimiterFilter.java # Filtro para limitação de taxa
│   │       └── SpringApiGatewayApplication.java # Classe principal
│   └── resources/
│       ├── application.yml              # Configuração principal
│       ├── banner.txt                   # Banner personalizado
│       └── logback-spring.xml           # Configuração de logging
└── test/
    ├── java/
    │   └── br/dev/leandro/spring/cloud/
    │       ├── CircuitBreakerTests.java   # Testes de circuit breaker
    │       ├── GatewayRoutingTests.java   # Testes de roteamento
    │       └── TestSecurityConfig.java    # Configuração de segurança para testes
    └── resources/
        └── application-test.yml         # Configuração para testes
```

## Funcionalidades Principais

### Roteamento de API
- Roteamento baseado em path para diferentes microserviços
- Reescrita de paths para compatibilidade com serviços internos
- Balanceamento de carga entre instâncias de serviços

### Segurança
- Autenticação via OAuth2/JWT com Keycloak
- Autorização baseada em roles e permissões
- Proteção contra ataques comuns

### Limitação de Taxa (Rate Limiting)
- Limitação de taxa baseada em IP usando Redis
- Configuração personalizada por rota
- Suporte a diferentes algoritmos de limitação

### Resiliência
- Circuit Breaker para proteção contra falhas em cascata
- Timeouts configuráveis
- Fallbacks para respostas em caso de falha

### Observabilidade
- Logging estruturado com correlationId
- Métricas para monitoramento de performance
- Rastreamento distribuído com OpenTelemetry

## Configuração e Instalação

### Pré-requisitos
- Java 21
- Maven
- Docker e Docker Compose (para ambiente local)

### Instalação Local

1. Clone o repositório:
```bash
git clone https://github.com/seu-usuario/spring-api-gateway.git
cd spring-api-gateway
```

2. Inicie os serviços dependentes com Docker Compose:
```bash
docker-compose up -d
```

3. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

### Configuração

As principais configurações estão no arquivo `application.yml`. Alguns exemplos de configurações importantes:

#### Configuração de Porta
```yaml
server:
  port: 9999
```

#### Configuração de Rota
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://${USER_SERVICE_URL:localhost}:8091
          predicates:
            - Path=/api/users/**
          filters:
            - RewritePath=/api/users/(?<segment>.*), /users/${segment}
```

#### Configuração de Rate Limiting
```yaml
spring:
  cloud:
    gateway:
      redis-rate-limiter:
        replenish-rate: 5
        burst-capacity: 10
```

#### Configuração de Segurança
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://${KEYCLOAK_URL:localhost}:8443/realms/event-management
```

## Uso

### Exemplo de Requisição
```bash
# Requisição para o serviço de usuários através do gateway
curl -X GET http://localhost:9999/api/users/profile \
  -H "Authorization: Bearer <seu-token-jwt>"
```

### Monitoramento
Acesse os endpoints do Actuator para monitoramento:
```
http://localhost:9999/actuator/health
http://localhost:9999/actuator/metrics
http://localhost:9999/actuator/prometheus
```

## Implantação

### Docker
O projeto inclui um Dockerfile para criação de imagens Docker:

```bash
./mvnw clean package -Pdocker
```

### Kubernetes
Usando JKube para implantação em Kubernetes:

```bash
./mvnw k8s:build k8s:resource k8s:apply
```

## Contribuição
Contribuições são bem-vindas! Sinta-se à vontade para abrir issues ou enviar pull requests.

## Licença
Este projeto é licenciado sob a licença MIT - veja o arquivo LICENSE para detalhes.
