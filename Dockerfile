FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jre-alpine
ENV TZ=Asia/Seoul
ARG JAR_FILE_NAME

COPY --from=builder /app/build/libs/${JAR_FILE_NAME} /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]