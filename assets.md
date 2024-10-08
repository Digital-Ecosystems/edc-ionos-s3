
# IONOS's Asset Definitions

This document describes how to define the asset at the creation and transfer process stages.

## Asset's Registrations 
The asset registration aims to specify which file/folder we want to share. We can also use pattern filters, these filters will be used to choose which files/folders to share/exclude.

### Requirements


| Parameter        | Description                                                                                                                                                                                                                                                                                                                                 | Mandatory              |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| `region`         | IONOS S3 endpoint region. Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/s3-endpoints)  for further information.| no, default value = de |
| `bucketName`     | IONOS S3 bucket name.   Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/concepts/buckets) for further information.| yes                    |
| `blobName`       | File name or path to folder| yes                    |
| `filterIncludes` | `filterIncludes` use regular expression that will be used to select the file name pattern from the asset's blobName that will be copied during the transfer <br/> * do not consider the blobName in the expression, but the path from it. example: blobName = folder1, filterIncludes=file1.csv, the file foloder1/file1.csv will be copied| no                     |
| `filterExcludes` | `filterExcludes` use regular expression that will be used to select the file name pattern from the asset's blobName that will NOT be copied during the transfer <br/>| no                     |

Note:  if `filterIncludes` and  `filterExcludes` parameters are satisfied, the files to be copied will be selected using the `filterIncludes` and after that selected list, the files that have the pattern defined in the `filterExcludes` will be ignored.


## Example

```json
"dataAddress":{
  "type": "IonosS3", //from EDC
  "region": "de",
  "bucketName": "mybucket",
  "blobName": "folder1/",
  "filterIncludes": "file1.csv",
  "filterExcludes": "file2.csv",
  "keyName": "mykey" //from EDC
}
```

## Assets transfer
The transfer of assets aims to transfer the files/folders from one connector to another connector.

### Requirements


| Parameter     | Description                                                                                                                                  | Mandatory              |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| `region`      | IONOS S3 endpoint region. Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/s3-endpoints)  for further information.| no, default value = de |
| `bucketName`  | IONOS S3 bucket name.   Refer to  [docs](https://docs.ionos.com/cloud/managed-services/s3-object-storage/concepts/buckets) for further information| yes                    |
| `path`        | Path of destination where the file/folder will be placed. </br>  *if the path not filled, the file will be placed in the root of the bucket. | no                     |


## Example

```json
"dataDestination":{
  "type": "IonosS3", //from EDC
  "region": "de",
  "bucketName": "mybucket",
  "path": "folder2/",
  "keyName": "mykey" //from EDC
}
```