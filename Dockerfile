# Stage 1: Build All Modules
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Ye line aapke saare folders (common, admin, etc.) ko copy karegi
COPY . .

# DHYAN DEIN: Yahan 'install' likhna zaroori hai taaki 'common' locally save ho jaye
RUN mvn clean install -DskipTests

# Stage 2: Run the Admin App
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Yahan '/app/admin/target' likha hai. Agar aapke admin folder ka naam kuch aur hai 
# (jaise 'admin-backend' ya 'Quantifyre_Admin'), toh 'admin' ki jagah wo naam daalein.
COPY --from=build /app/admin/target/*.jar app.jar

# Hugging Face Security Requirements
RUN chmod 777 /app && chmod 777 app.jar
USER 1000

EXPOSE 7860

# Run the application
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
