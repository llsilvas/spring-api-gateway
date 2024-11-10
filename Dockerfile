FROM eclipse-temurin:21 as builder
# First stage : Extract the layers
WORKDIR /@project.name@

ADD ./ /@project.name@

ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar

RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-jammy as final
# Cria o usuário e grupo spring
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Instala tzdata, bash e curl para dockerize
RUN apt-get update && apt-get install -y tzdata bash curl && rm -rf /var/lib/apt/lists/*

# Define o timezone desejado enquanto ainda é root
RUN ln -snf /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && echo "America/Sao_Paulo" > /etc/timezone

# Define o diretório de trabalho
WORKDIR /@project.name@

## Second stage : Copy the extracted layers
COPY --from=builder /@project.name@/dependencies/ ./
COPY --from=builder /@project.name@/spring-boot-loader/ ./
COPY --from=builder /@project.name@/snapshot-dependencies/ ./
COPY --from=builder /@project.name@/application/ ./
COPY --from=builder /@project.name@/target/*.jar app.jar

# Copia o script wait-for-it.sh
COPY wait-for-it.sh .

# Torna o script executável
RUN chmod +x wait-for-it.sh

# Instala o dockerize
ENV DOCKERIZE_VERSION v0.6.1
RUN curl -LO https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz \
    && rm dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz

# Altera o dono do diretório de trabalho
RUN chown -R spring:spring /@project.name@

# Altera o usuário para o usuário não root
USER spring:spring

# Define variáveis de ambiente
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""
ENV LOKI_URL="loki"
ENV KEYCLOAK_URL=""

EXPOSE 9999

# Define o ENTRYPOINT com dockerize para aguardar o serviço e iniciar a aplicação
ENTRYPOINT ["dockerize", "-wait", "tcp://spring-config-server:8888", "-timeout", "60s", "java", "-jar", "app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
