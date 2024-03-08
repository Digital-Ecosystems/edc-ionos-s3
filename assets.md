
# IONOS's Asset Definitions

This document explains how the asset registration and transfer works.

## Asset's Registrations 
The asset registration aims to indicate which file/folder we want to share, we can also use pattern filters, these filters will be used to choose which files/folders to share/exclude.

### Requirements


| Parameter        | Description                                                                                                                                                                                                                                                                                                                                                                                                                             | Mandatory |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `storage`        | IONOS S3 endpoint address. Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/s3-endpoints)  for further information.                                                                                                                                                                                                                                                                                     | yes       |
| `bucketName`     | IONOS S3 bucket name.   Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/concepts/buckets) for further information.                                                                                                                                                                                                                                                                                     | yes       |
| `blobName`       | Name or path of files/folders                                                                                                                                                                                                                                                                                                                                                                                                           | yes       |
| `filterIncludes` |filterIncludes use regular expression that will be used to select the file name pattern from the asset's blobName that will be copied during the transfer <br/> * do not consider the blobName in the expression, but the path from it. example: blobName = folder1, filterIncludes=file1.csv, the file foloder1/file1.csv will be copied| no        |
| `filterExcludes` | filterExcludes use regular expression that will be used to select the file name pattern from the asset's blobName that will NOT be copied during the transfer <br/>  * if both properties are filled, the files to be copied will be selected using the filterIncludes and after that selected list, the files that have the pattern defined in the filterExcludes will be removed | no        |



## Example

```json
{
  "type": "IonosS3",
  "storage": "s3-eu-central-1.ionoscloud.com",
  "bucketName": "mybucket",
  "blobName": "folder1/",
  "filterIncludes": "file1.csv",
  "filterExcludes": "file2.csv",
  "keyName": "mykey"
}
```

## Assets transfer
The transfer of assets aims to transfer the files/folders from one connector to another connector.

### Requirements


| Parameter    | Description                                                                                                                                         | Mandatory |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| `storage`    | IONOS S3 endpoint address. Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/s3-endpoints)  for further information. | yes       |
| `bucketName` | IONOS S3 bucket name.   Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/concepts/buckets) for further information. | yes       |
| `path`       | Path of destination where the files/folders will be placed.                                                                                                                                     | yes       |


## Example

```json
{
  "type": "IonosS3",
  "storage": "s3-eu-central-1.ionoscloud.com",
  "bucketName": "mybucket",
  "path": "folder2/",
  "keyName": "mykey"
}
```