FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar

FROM eclipse-temurin:23-jdk-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]