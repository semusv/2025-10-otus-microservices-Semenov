FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY api-gateway/build.gradle ./api-gateway/
COPY api-gateway/src ./api-gateway/src
RUN gradle :api-gateway:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

