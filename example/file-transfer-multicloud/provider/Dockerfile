# -buster is required to have apt available
FROM openjdk:11-jre-slim-buster

# by default curl is not available, so install it
RUN apt update && apt install curl -y

WORKDIR /app


# Copy config
COPY ./resources /resources
# Copy jar
COPY ./build/libs/dataspace-connector.jar /app

ENTRYPOINT java  -Dedc.fs.config=./provider-config.properties -jar dataspace-connector.jar