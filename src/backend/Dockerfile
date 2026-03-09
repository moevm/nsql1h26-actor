# Сборка: сначала JAR на хосте (из backend): ./gradlew bootJar -x test
# Затем образ из корня src: docker build -f backend/Dockerfile .
# Запуск: docker run -p 8080:8080 -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/nosql_db <image>

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Порт приложения (из application.yaml)
EXPOSE 8080

# Копируем исполняемый JAR (соберите: cd backend && ./gradlew bootJar -x test)
COPY backend/build/libs/NOSQL-0.0.1-SNAPSHOT.jar app.jar

# MongoDB и JWT настраиваются через переменные окружения при запуске контейнера
ENV SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/nosql_db

ENTRYPOINT ["java", "-jar", "app.jar"]
