# ==========================================
# ETAPA 1: Compilação (Build)
# ==========================================
FROM eclipse-temurin:21.0.11_10-jdk AS builder
WORKDIR /app

# Copia apenas os arquivos do Maven Wrapper e o pom.xml primeiro
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copia o código-fonte e compila gerando o arquivo JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# ETAPA 2: Execução (Run)
# ==========================================
FROM eclipse-temurin:21.0.11_10-jre-alpine
WORKDIR /app

# Cria um usuário do sistema para evitar rodar a aplicação como root
# UID/GID numéricos permitem ao Kubernetes validar runAsNonRoot
RUN addgroup -S -g 1001 spring && adduser -S -u 1001 -G spring spring
USER 1001:1001

ENV SPRING_PROFILES_ACTIVE=docker

# Commit do Git que gerou a imagem, exposto em GET /api/v1/info
ARG APP_COMMIT_SHA=local
ENV APP_COMMIT_SHA=${APP_COMMIT_SHA}

# Copia apenas o JAR gerado na etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8089

# Comando de inicialização otimizado para containers
ENTRYPOINT ["java", "-jar", "app.jar"]