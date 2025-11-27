FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew :natk-antivirus:bootJar --no-daemon

FROM eclipse-temurin:23-jdk-alpine
WORKDIR /app

COPY --from=build /app/natk-antivirus/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]