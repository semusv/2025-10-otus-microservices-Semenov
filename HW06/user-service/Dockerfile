FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY user-service/build.gradle ./user-service/
COPY user-service/src ./user-service/src
RUN gradle :user-service:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

