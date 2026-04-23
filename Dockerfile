# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Pehle common module copy karo aur install karo
# Isse 'common' library aapki Maven local repo mein chali jayegi
COPY common/ ./common/
RUN mvn clean install -f common/pom.xml

# 2. Phir admin module copy karo
COPY admin/ ./admin/

# 3. Ab admin build karo
# 'admin' ab 'common' ko local repo se utha lega
RUN mvn clean package -f admin/pom.xml -DskipTests

# Stage 2: Run (Final Image)
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Step 1: Copy the jar file from the build stage (admin/target se uthayega)
COPY --from=build /app/admin/target/*.jar app.jar

# Step 2: Permissions
RUN chmod 777 /app && chmod 777 app.jar

# Step 3: Switch to UID 1000
USER 1000

EXPOSE 7860

# Step 4: Run
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
