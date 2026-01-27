# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-21 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/bizflow-gateway-*.jar app.jar
COPY wait-for-services.sh wait-for-services.sh
RUN chmod +x wait-for-services.sh
EXPOSE 8000
ENTRYPOINT ["./wait-for-services.sh"]
