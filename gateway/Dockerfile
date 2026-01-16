# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-21 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/bizflow-gateway-*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java","-jar","app.jar"]
