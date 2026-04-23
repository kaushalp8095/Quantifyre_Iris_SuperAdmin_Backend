# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Sab kuch (pom.xml, libs/, src/) copy karo
COPY . .

# Maven build run karo
# 'maven-install-plugin' validate phase mein common.jar ko install kar dega
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Step 1: Copy the jar file from build stage (target folder se)
# * wildcard use kiya hai taaki jar ka naam kuch bhi ho toh utha le
COPY --from=build /app/target/*.jar app.jar

# Step 2: Permissions
RUN chmod 777 /app && chmod 777 app.jar

# Step 3: Switch to UID 1000
USER 1000

EXPOSE 7860

# Step 4: Run
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
