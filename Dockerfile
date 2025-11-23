# Multi-stage Dockerfile for Speech-to-Text Service
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

# Install required dependencies for Vosk
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for models and temp files
RUN mkdir -p /models /tmp/transcriptions /var/log

# Download Vosk model (using small English model for demo)
# For production, you might want to bake different models or mount them
RUN wget -q https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip -O /tmp/model.zip \
    && unzip /tmp/model.zip -d /models \
    && rm /tmp/model.zip

# Set environment variables
ENV MODEL_PATH=/models/vosk-model-small-en-us-0.15
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xmx2g -Xms512m"

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/transcribe/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
