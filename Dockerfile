# Simple Docker image for pre-built Spring Boot JAR

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the pre-built JAR file (built by GitHub Actions)
COPY target/baleen-*.jar app.jar

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/status || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0"

# If KEYSTORE_B64 is provided (deploy injects from Key Vault), decode it to a
# file and point KEYSTORE_LOCATION at it. Otherwise the app falls back to the
# bundled self-signed localdev keystore on the classpath.
ENTRYPOINT ["sh", "-c", "if [ -n \"$KEYSTORE_B64\" ]; then echo \"$KEYSTORE_B64\" | base64 -d > /app/keystore.p12 && export KEYSTORE_LOCATION=file:/app/keystore.p12; fi; exec java $JAVA_OPTS -jar app.jar"]