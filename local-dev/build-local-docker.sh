#!/bin/bash

# Build Docker image locally for testing

set -e

cd "$(dirname "$0")/.."

IMAGE_NAME="baleen-server"
CONTAINER_NAME="baleen-local"

echo "🔨 Building Baleen application..."

# Build the application
mvn -DskipTests clean install

echo "🐳 Building Docker image locally..."

# Build Docker image locally
docker build --platform linux/amd64 -f Dockerfile -t $IMAGE_NAME .

echo "🚀 Running container locally..."

# Stop and remove existing container if running
if docker ps -q -f name=$CONTAINER_NAME > /dev/null; then
    echo "Stopping existing container..."
    docker stop $CONTAINER_NAME
fi

if docker ps -aq -f name=$CONTAINER_NAME > /dev/null; then
    echo "Removing existing container..."
    docker rm $CONTAINER_NAME
fi

# Run the container locally
docker run -d \
  --name $CONTAINER_NAME \
  -p 9090:8080 \
  $IMAGE_NAME

if [ $? -eq 0 ]; then
    echo
    echo "✅ Container started successfully!"
    echo
    echo "Application URLs:"
    echo "HTTP:  http://localhost:9090"
    echo
    echo "Login with: demo/demo"
    echo
    echo "To view logs: docker logs -f $CONTAINER_NAME"
    echo "To stop: docker stop $CONTAINER_NAME"
else
    echo "❌ Failed to start container!"
    exit 1
fi