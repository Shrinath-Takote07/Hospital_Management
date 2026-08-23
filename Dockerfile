FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the JAR (will find it after mvn package)
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]