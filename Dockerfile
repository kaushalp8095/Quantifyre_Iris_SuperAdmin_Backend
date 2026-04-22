# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# ✅ Correct path
COPY --from=build /app/target/*.jar app.jar

RUN chmod 777 /app && chmod 777 app.jar

USER 1000

EXPOSE 7860

ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
