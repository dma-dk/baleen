# Baleen XML Web Server

This project is a simple XML web server that accepts XML documents via POST requests and displays them in a web interface. It's built with Java 21 and uses Maven for building and Docker for containerization.

## Prerequisites

- Java Development Kit (JDK) 21
- Maven 3.6+
- Docker
- Azure CLI (for uploading to Azure Container Registry)

## Building the Project

1. Clone this repository:
   ```
   git clone https://github.com/yourusername/baleen-xml-server.git
   cd baleen-xml-server
   ```

2. Build the project using Maven:
   ```
   mvn clean package
   ```

This will compile the Java code and create a JAR file in the `target` directory.

## Running Locally

You can run the application directly using Java:

```
java -jar target/xml-web-server-1.0-SNAPSHOT.jar
```

The server will start on `http://localhost:80`.

## Building and Running with Docker

1. Build the Docker image:
   ```
   mvn clean package jib:dockerBuild
   ```

2. Run the Docker container:
   ```
   docker run -p 80:80 sfs0cr.azurecr.io/baleen-server
   ```

The server will be accessible at `http://localhost:80`.

## Uploading to Azure Container Registry

1. Make sure you're logged in to Azure:
   ```
   az login
   ```

2. Log in to the Azure Container Registry:
   ```
   az acr login --name sfs0cr
   ```

3. Build and push the image to Azure Container Registry:
   ```
   mvn clean package jib:build
   ```

This will build the Docker image and push it to `sfs0cr.azurecr.io/baleen-server`.

## Usage

- To view the web interface, open a browser and go to `http://localhost:80`.

- To submit an XML document, use a POST request to `http://localhost:80/submit`. Don't forget to include the authentication token in the `X-Auth-Token` header:

  ```
  curl -X POST \
       -H "Content-Type: application/xml" \
       -H "X-Auth-Token: BaleenIsGreat" \
       -d "<root><element>Test XML</element></root>" \
       http://localhost:80/submit
  ```

- The web interface will automatically update to show new XML documents as they are submitted.

## Troubleshooting

- If you encounter permission issues when pushing to Azure Container Registry, ensure you have the necessary permissions and that you're correctly logged in.
- If the application doesn't start, check that port 80 isn't being used by another application.
