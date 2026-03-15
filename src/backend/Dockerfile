FROM gradle:9.1.0-jdk24 AS builder

WORKDIR /app

COPY src/backend/build.gradle.kts src/backend/settings.gradle.kts ./
COPY src/backend/gradle ./gradle

RUN gradle dependencies --no-daemon

COPY src/spec ./spec

COPY src/backend/src ./src
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV SPRING_DATA_MONGODB_URI=""

ENTRYPOINT ["java", "-jar", "app.jar"]
