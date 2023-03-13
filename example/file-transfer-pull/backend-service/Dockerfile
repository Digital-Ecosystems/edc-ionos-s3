# -buster is required to have apt available
FROM openjdk:11-jre-slim-buster

# by default curl is not available, so install it
RUN apt update && apt install curl -y

WORKDIR /app

# Copy jar
COPY ./build/libs/backend-service.jar /app

ENTRYPOINT java  -jar ./backend-service.jar