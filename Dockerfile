# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Saara code copy karein (Common aur Admin dono aayenge)
COPY . .

# Pehle poore project ko ek sath install karein (Isse common module pehle build hoga)
RUN mvn clean install -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# DHYAN DEIN: Kyunki ye multi-module hai, admin ki jar file 'admin' folder ke andar banegi
COPY --from=build /app/admin/target/*.jar app.jar

# Permissions aur User (Hugging Face Requirement)
RUN chmod 777 /app && chmod 777 app.jar
USER 1000

EXPOSE 7860

# Run the application
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
