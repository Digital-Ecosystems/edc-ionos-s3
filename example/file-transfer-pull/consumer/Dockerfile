# -buster is required to have apt available
FROM openjdk:11-jre-slim-buster

# by default curl is not available, so install it
RUN apt update && apt install curl -y

WORKDIR /app


# Copy config
COPY ./resources/ /app
# Copy jar
COPY ./build/libs/http-pull-connector.jar /app

ENTRYPOINT java -Dedc.keystore=./cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=./consumer-vault.properties -jar -Dedc.fs.config=./consumer-configuration.properties ./http-pull-connector.jar