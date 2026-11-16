FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY result-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
