# ==========================================
# ETAPA 1: Compilação (Build)
# ==========================================
FROM eclipse-temurin:21.0.11_10-jdk@sha256:98155457fcdbeb0987472e8abf721657b698ede407aad52ac24ae42618c0931c AS builder
WORKDIR /app

# Copia apenas os arquivos do Maven Wrapper e o pom.xml primeiro
COPY .mvn/ .mvn
COPY pom.xml ./
COPY --chmod=0755 mvnw ./
RUN ./mvnw dependency:go-offline -B

# Copia o código-fonte e compila gerando o arquivo JAR
COPY src/main ./src/main
# A suíte e o gate de qualidade são executados antes do build da imagem.
RUN ./mvnw clean package -Dmaven.test.skip=true -B

# ==========================================
# ETAPA 2: Execução (Run)
# ==========================================
FROM eclipse-temurin:21.0.11_10-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c
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
EXPOSE 8080

# Comando de inicialização otimizado para containers
ENTRYPOINT ["java", "-jar", "app.jar"]
