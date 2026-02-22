# Dockerfile for Accordion Chat Backend
# Multi-stage build for smaller image size

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy all backend files
COPY backend/pom.xml .
COPY backend/src ./src

# Build the application (this will download dependencies as needed)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create data directory for H2 database with proper ownership
RUN mkdir -p /app/data && chown -R spring:spring /app/data

# Copy the built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/messages || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
