FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

FROM mcr.microsoft.com/playwright/java:v1.61.0-noble
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/cricory-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/cricory-backend.jar"]
