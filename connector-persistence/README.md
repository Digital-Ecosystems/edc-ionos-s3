# Running EDC with IONOS S3 Extension - Docker

This document explain how to deploy an EDC with IONOS S3 Extension and Persistence using Docker.


## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 11 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;
- PosgreSQL database.

## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../README.md).

### Configuration

Just check the `Configuration` section of the example [readme](../example/README.md) to configure your IONOS S3 storage.

Open the `resources/config.properties` file and insert the key and the secret of your IONOS S3 storage and the token.

### Import the initial database
```
psql -h <YOUR POSTGRES HOST> -p <YOUR POSTGRES PORT> -U <YOUR POSTGRES DATABASE> < ../deployment/terraform/db-scripts/init.sql
```

## Building and running the docker

```bash
docker build -t edc-ionos-s3 .
docker-compose up
```
