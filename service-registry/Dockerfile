FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY service-registry/pom.xml ./pom.xml
RUN --mount=type=cache,id=maven-m2,target=/root/.m2,sharing=locked \
    mvn -B -q dependency:go-offline || true

COPY service-registry/src ./src
RUN --mount=type=cache,id=maven-m2,target=/root/.m2,sharing=locked \
    mvn -B -DskipTests package && \
    JAR_PATH="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' ! -name '*-plain.jar' | head -n 1)" && \
    cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build --chown=appuser:appgroup /workspace/app.jar /app/app.jar

USER appuser

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
