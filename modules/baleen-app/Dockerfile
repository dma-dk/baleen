FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact from build stage
COPY /target/*.jar app.jar

# Environment variables
ENV JAVA_OPTS=""

# Expose the port your application runs on
EXPOSE 8080

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]