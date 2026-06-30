#!/bin/bash

# Run the Docker container locally on port 9090

CONTAINER_NAME="baleen-local"
IMAGE="sfs0cr.azurecr.io/baleen-server:latest"

echo "🐳 Running Baleen Docker container locally..."
echo "Port mapping: localhost:9090 -> container:8080"
echo

# Stop and remove existing container if running
if docker ps -q -f name=$CONTAINER_NAME > /dev/null; then
    echo "Stopping existing container..."
    docker stop $CONTAINER_NAME
fi

if docker ps -aq -f name=$CONTAINER_NAME > /dev/null; then
    echo "Removing existing container..."
    docker rm $CONTAINER_NAME
fi

# Run the container
echo "Starting new container..."
docker run -d \
  --name $CONTAINER_NAME \
  -p 9090:8080 \
  -p 9443:8443 \
  $IMAGE

if [ $? -eq 0 ]; then
    echo
    echo "✅ Container started successfully!"
    echo
    echo "Application URLs:"
    echo "HTTP:  http://localhost:9090"
    echo "HTTPS: https://localhost:9443"
    echo
    echo "Login with: demo/demo"
    echo
    echo "To view logs: docker logs -f $CONTAINER_NAME"
    echo "To stop: docker stop $CONTAINER_NAME"
else
    echo "❌ Failed to start container!"
    exit 1
fi