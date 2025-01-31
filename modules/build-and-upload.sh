# Remember to login to the repository before pushing
# az acr login --name sfs0cr

#mvn -DskipTests clean install && \
#(cd baleen-app && \
#docker build --platform linux/amd64 -f src/main/Docker/Dockerfile.jvm -t sfs0cr.azurecr.io/baleen-server . && \
#docker push sfs0cr.azurecr.io/baleen-server)

# Run Maven from parent directory
mvn -DskipTests clean install

# Run Docker commands referencing baleen-app directory without cd-ing into it
docker build --platform linux/amd64 -f baleen-app/Dockerfile  -t sfs0cr.azurecr.io/baleen-server baleen-app

docker push sfs0cr.azurecr.io/baleen-server