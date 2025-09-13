FROM openjdk:17-jdk-slim

WORKDIR /app

COPY money-transfer-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]