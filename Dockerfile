FROM gradle:9.4.0-jdk21 AS build
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl && update-ca-certificates
WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
