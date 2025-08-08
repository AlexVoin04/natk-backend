FROM eclipse-temurin:23-jdk AS build

WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar

FROM eclipse-temurin:23-jdk
WORKDIR /app

RUN apt-get update && \
    apt-get install -y libreoffice && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]