# Stage 1: Builder
FROM gradle:8.1.1-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle clean build -x test

# Stage 2: Runner
FROM openjdk:17-jdk-slim
WORKDIR /app
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring
COPY --from=builder /app/money-transfer-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]