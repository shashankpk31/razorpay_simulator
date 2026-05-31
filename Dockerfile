# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application (skip tests to speed up the deploy)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image (Lightweight OpenJDK)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 9000

# Run the application with optimized memory settings for Render's Free Tier
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]