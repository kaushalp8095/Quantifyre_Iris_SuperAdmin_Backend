# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 🔴 Step 1: Copy pom + libs first (important for caching & dependency)
COPY pom.xml .
COPY libs ./libs

# 🔴 Step 2: Download dependencies (system scope jar bhi pick karega)
RUN mvn dependency:go-offline

# 🔴 Step 3: Copy rest of code
COPY src ./src

# 🔴 Step 4: Build jar
RUN mvn clean package -DskipTests


# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy jar
COPY --from=build /app/target/*.jar app.jar

# Permissions
RUN chmod 777 /app && chmod 777 app.jar

USER 1000

EXPOSE 7860

ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
