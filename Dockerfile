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
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENV SPRING_PROFILES_ACTIVE=docker

# Copia apenas o JAR gerado na etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando de inicialização otimizado para containers
ENTRYPOINT ["java", "-jar", "app.jar"]