# File transfer using pull method 

 This example shows how to exchange a data file between two EDC's. It is based on a [sample](https://github.com/eclipse-edc/Samples/blob/main/transfer/transfer-06-consumer-pull-http/README.md) of the official EDC respository that uses the HTTP to pull the data.


## Requirements

You will need the following:
- IONOS account;
- Java Development Kit (JDK) 11 or higher;
- Docker;
- GIT;
- Linux shell or PowerShell;

## Deployment

### Building the project

Just check the `Building and Running` section of the previous [readme](../../README.md).

### Configuration
In order to configure this sample, please follow this steps:
(We will use the [DCD](https://dcd.ionos.com))
1) Create a S3 Key Management: access the `Storage\Object Storage\S3 Key Management` option and generate a Key. Keep the key and the secret;
2) Create the required buckets: access the `Storage\Object Storage\S3 Web Console` option and create two buckets: company1;
3) Upload a file named `device1-data.csv` into the company1 bucket. You can use the `example/file-transfer-pull/device1-data.csv`;
4) Open the `example/file-transfer-pull/provider/resources/provider-config.properties` file and insert the key and the secret (step 1);

Note: by design, S3 technology allows only unique names for the buckets. You may find an error saying that the bucket name already exists.

## Usage

Local execution:
```bash
java -Dedc.keystore=example/file-transfer-pull/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=example/file-transfer-pull/consumer/resources/consumer-vault.properties -Dedc.fs.config=example/file-transfer-pull/consumer/resources/consumer-config.properties -jar example/file-transfer-pull/consumer/build/libs/dataspace-connector.jar

java -Dedc.keystore=example/file-transfer-pull/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=example/file-transfer-pull/provider/resources/provider-vault.properties -Dedc.fs.config=example/file-transfer-pull/provider/resources/provider-config.properties -jar example/file-transfer-pull/provider/build/libs/dataspace-connector.jar

java -jar ./example/file-transfer-pull/backend-service/build/libs/http-pull-connector.jar
```

or

```bash
docker compose -f "docker-compose.yml" up --build
```
If you use docker to do the deployment of this example, don't forget to replace `localhost` with `consumer` and `provider` in the curls below.

We will have to call some URL's in order to transfer the file:
  
  (If you want, you can execute the `runDemo2.sh` script and jump to the step `10`)

1) Register the data planes for the provider
```console
curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-provider-dataplane",
   "url": "http://localhost:19192/control/transfer",
   "allowedSourceTypes": [ "HttpData", "IonosS3" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData" ],
   "properties": {
     "publicApiUrl": "http://localhost:19291/public/"
   }
 }' \
     -X POST "http://localhost:19193/api/v1/data/instances"
```

2) Register the data planes for the consumer
```console
curl -H 'Content-Type: application/json' \
     -d '{
   "edctype": "dataspaceconnector:dataplaneinstance",
   "id": "http-pull-consumer-dataplane",
   "url": "http://localhost:29192/control/transfer",
   "allowedSourceTypes": [ "HttpData" ],
   "allowedDestTypes": [ "HttpProxy", "HttpData" ],
   "properties": {
     "publicApiUrl": "http://localhost:29291/public/"
   }
 }' \
     -X POST "http://localhost:29193/api/v1/data/instances"
```

3) Asset creation for the consumer
```console
curl -d '{
           "asset": {
             "properties": {
               "asset:prop:id": "assetId",
               "asset:prop:name": "product description",
               "asset:prop:contenttype": "application/json"
             }
           },
           "dataAddress": {
             "properties": {
               "name": "Test asset",
               "baseUrl": "https://jsonplaceholder.typicode.com/users",
               "type": "HttpData"
             }
           }
         }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/assets \
         -s | jq
```

4) Policy creation
```console
curl -d '{
           "id": "aPolicy",
           "policy": {
             "uid": "231802-bb34-11ec-8422-0242ac120002",
             "permissions": [
               {
                 "target": "assetId",
                 "action": {
                   "type": "USE"
                 },
                 "edctype": "dataspaceconnector:permission"
               }
             ],
             "@type": {
               "@policytype": "set"
             }
           }
         }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/policydefinitions \
         -s | jq
```

5) Contract creation
```console
curl -d '{
           "id": "1",
           "accessPolicyId": "aPolicy",
           "contractPolicyId": "aPolicy",
           "criteria": []
         }' -H 'content-type: application/json' http://localhost:19193/api/v1/data/contractdefinitions \
         -s | jq
```

6) Fetching the catalog
```console
curl -X POST "http://localhost:29193/api/v1/data/catalog/request" \
--header 'Content-Type: application/json' \
--data-raw '{
  "providerUrl": "http://localhost:19194/api/v1/ids/data"
}'
```

7) Contract negotiation
```console
curl -d '{
           "connectorId": "http-pull-provider",
           "connectorAddress": "http://localhost:19194/api/v1/ids/data",
           "protocol": "ids-multipart",
           "offer": {
             "offerId": "1:50f75a7a-5f81-4764-b2f9-ac258c3628e2",
             "assetId": "assetId",
             "policy": {
               "uid": "231802-bb34-11ec-8422-0242ac120002",
               "permissions": [
                 {
                   "target": "assetId",
                   "action": {
                     "type": "USE"
                   },
                   "edctype": "dataspaceconnector:permission"
                 }
               ],
               "@type": {
                 "@policytype": "set"
               }
             }
           }
         }' -X POST -H 'content-type: application/json' http://localhost:29193/api/v1/data/contractnegotiations \
         -s | jq
```

8) Contract agreement

Copy the value of the `id` from the response of the previous curl into this curl and execute it.
```console
curl -X GET "http://localhost:29193/api/v1/data/contractnegotiations/{<ID>}" \
    --header 'Content-Type: application/json' \
    -s | jq	
```

9) Transfering the asset

Copy the value of the `contractAgreementId` from the response of the previous curl into this curl and execute it.
```console
curl -X POST "http://localhost:29193/api/v1/data/transferprocess" \
    --header "Content-Type: application/json" \
    --data '{
                "connectorId": "http-pull-provider",
				"connectorAddress": "http://localhost:19194/api/v1/ids/data",
                "contractId": "<CONTRACT AGREEMENT ID>",
                "assetId": "assetId",
                "managedResources": "false",
				"dataDestination": {
				"properties": {
				  "type":"HttpProxy"
				}  
				}
			}' \
    -s | jq
```

10) Getting authcode
Go to the backend log and copy the authcode.
Example of an authcode:
```console
eyJhbGciOiJSUzI1NiJ9.eyJleHAiOjE2NzgxMjEyMjQsImRhZCI6IntcInByb3BlcnRpZXNcIjp7XCJhdXRoS2V5XCI6XCJBdXRob3JpemF0aW9uXCIsXCJiYXNlVXJsXCI6XCJodHRwOi8vbG9jYWxob3N0OjE5MjkxL3B1YmxpYy9cIixcImF1dGhDb2RlXCI6XCJleUpoYkdjaU9pSlNVekkxTmlKOS5leUpsZUhBaU9qRTJOemd4TWpFeU1qTXNJbVJoWkNJNkludGNJbkJ5YjNCbGNuUnBaWE5jSWpwN1hDSmlZWE5sVlhKc1hDSTZYQ0pvZEhSd2N6b3ZMMnB6YjI1d2JHRmpaV2h2YkdSbGNpNTBlWEJwWTI5a1pTNWpiMjB2ZFhObGNuTmNJaXhjSW01aGJXVmNJanBjSWxSbGMzUWdZWE56WlhSY0lpeGNJblI1Y0dWY0lqcGNJa2gwZEhCRVlYUmhYQ0o5ZlNJc0ltTnBaQ0k2SWpFNk9EQXdaREppWXpNdFpUVXpPQzAwTjJVMkxXRTVNbUV0TUdFeU4yUXdabU5rWVRoakluMC5aSUg4TEhoR2l4UC1WRzBMLXY1MmFXSzlUYy1uOGUyUEgxZlNCSXBNNEZzdE03ejZOX1dGbmhnTDhVTURjUDFrdnJwRFJiWUV6Q1VHb3BRNE5aaTBKNzl6eVNTZWRCWmFVVVlIa3ZhMFdNUXo4dUhKMG1Jd0xnN2xMM0lDc1N4cDNMZ09LSjhyeVpJcGhxWTAzSG5ZOG9JMnlzWFdoTjBUaE02Y0lKWEZwbXRZLXNxSUh2d1FMaWZ2Rm9qUzM1d3VIZS1RdU9UMThnWElWZHc3cVJLWTVPTmdSWGhYQm04MUtxZThLRjk4QUNHbEc3Tk9Ea2lrSmpNWXlqVDAyRGJrWXVRd3Z2bDQ3VjhKZFJwSW1IcHkzS08tX1owOGdyVE1rSDRPUnVVaW5xRGkwbXZKQ0ZzX3ZOUG1jMnMtOUVzWjlFUE9ZMTk4anpoeGlNOTFMWXd4aVFcIixcInByb3h5TWV0aG9kXCI6XCJ0cnVlXCIsXCJwcm94eVF1ZXJ5UGFyYW1zXCI6XCJ0cnVlXCIsXCJwcm94eUJvZHlcIjpcInRydWVcIixcInR5cGVcIjpcIkh0dHBEYXRhXCIsXCJwcm94eVBhdGhcIjpcInRydWVcIn19IiwiY2lkIjoiMTo4MDBkMmJjMy1lNTM4LTQ3ZTYtYTkyYS0wYTI3ZDBmY2RhOGMifQ.I_0TxNPwCMdRfvkf6IUHGXPvTIHKoTaKpVKI-fmwYGnPsq-SDmmtcSZ60jadglIthS2DngfHZOY4mGVnjtarOTnG6x4y8RpWxRBC5YZRcmiCClTVIuYvrEPtpPAWx0bd0YRNCtKD_Wj_5P3t8nb3s8Ovh5G8qhK3byHrcg-UGpOhH8V5_zUS_bxh_mpIX42ytFm_kPMe9NDF3bvv9nH-wed53rleTHmZfTVgnaRvv7BDw2Yt97EbT-8FxwLQzpV8q7-8Lk-GkDTacpEEFfM-LFWsfhMFkIIHedVPfUoPwPNmqjWMUbFAynVr6SLnGBM9RFkuO31a0v2KWNi7p0Cp5Q
```

11) Confirmation of the asset transfer
```console
curl --location --request GET 'http://localhost:29291/public/' \
--header 'Authorization: <AUTHCODE>'
```

You will see the data of the file `device1-data.csv`.
