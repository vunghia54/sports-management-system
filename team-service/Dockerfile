FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY team-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
