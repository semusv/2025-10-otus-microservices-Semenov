FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY config-service/build.gradle ./config-service/
COPY config-service/src ./config-service/src
RUN gradle :config-service:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/config-service/build/libs/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]

