/*
 *  Copyright (c) 2022 IONOS
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      IONOS
 *
 */

package com.ionos.edc.extension.s3.api;

import com.ionos.edc.extension.s3.connector.MinioConnector;
import com.ionos.edc.extension.s3.connector.ionosapi.S3AccessKey;
import com.ionos.edc.extension.s3.connector.ionosapi.S3ApiConnector;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.eclipse.edc.spi.EdcException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class S3ConnectorApiImpl implements S3ConnectorApi {

    MinioConnector miniConnector = new MinioConnector();
    S3ApiConnector ionoss3Api = new S3ApiConnector();

    private final MinioClient minioClient;
    private final String region;
    private String token;
    private final Integer maxFiles;

    public S3ConnectorApiImpl(String endpoint, String accessKey, String secretKey, int maxFiles) {
        this.minioClient = miniConnector.connect(endpoint, accessKey, secretKey);
        this.region = getRegion(endpoint);
        this.token = "";
        this.maxFiles = maxFiles;
    }

    public S3ConnectorApiImpl(String endpoint, String accessKey, String secretKey, String token, int maxFiles) {
        this(endpoint, accessKey, secretKey, maxFiles);
        this.token = token;
    }

    @Override
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName.toLowerCase())) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName.toLowerCase())
                        .region(region)
                        .build());
            } catch (Exception e) {
                throw new EdcException("Creating bucket: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName.toLowerCase())
                    .region(region)
                    .build());
        } catch (Exception e) {
            throw new EdcException("Verifying if bucket exists - " + e.getMessage());
        }
    }

    @Override
    public void uploadObject(String bucketName, String objectName, ByteArrayInputStream stream) {
        if (!bucketExists(bucketName.toLowerCase())) {
            createBucket(bucketName.toLowerCase());
        }
       
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName.toLowerCase())
                    .region(region)
                    .object(objectName)
                    .stream(stream, stream.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new EdcException("Uploading parts: " + e.getMessage());
        }
    }

    @Override
    public ByteArrayInputStream getObject(String bucketName, String objectName) {
        if (!bucketExists(bucketName.toLowerCase())) {
            throw new EdcException("Bucket not found - " + bucketName);
        }

        var request = GetObjectArgs.builder()
                .bucket(bucketName.toLowerCase())
                .region(region)
                .object(objectName)
                .build();

        try (var response = minioClient.getObject(request)) {
            return new ByteArrayInputStream(response.readAllBytes());
        } catch (Exception e) {
            throw new EdcException("Getting file - " + e.getMessage());
        }
    }

    @Override
    public ByteArrayInputStream getObject(String bucketName, String objectName, long offset, long length) {
        if (!bucketExists(bucketName.toLowerCase())) {
            throw new EdcException("Bucket not found - " + bucketName);
        }

        var request = GetObjectArgs.builder()
                .bucket(bucketName.toLowerCase())
                .region(region)
                .object(objectName)
                .offset(offset)
                .length(length)
                .build();

        try (var response = minioClient.getObject(request)) {
            return new ByteArrayInputStream(response.readAllBytes());
        } catch (Exception e) {
            throw new EdcException("Getting file - " + e.getMessage());
        }
    }

    @Override
    public List<S3Object> listObjects(String bucketName, String objectName) {

        var objects = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName.toLowerCase())
                .region(region)
                .prefix(objectName)
                .recursive(true)
                .maxKeys(maxFiles)
                .build());

        return StreamSupport.stream(objects.spliterator(), false)
                .map(item -> {
                    try {
                        return item.get();
                    } catch (Exception e) {
                        throw new EdcException("Error fetching object", e);
                    }
                })
                .map(item -> new S3Object(item.objectName(), item.size()))
                .collect(Collectors.toList());
    }
    
    @Override
    public S3AccessKey createAccessKey() {
		try{
            return ionoss3Api.createAccessKey(token);
        } catch (Exception e) {
            throw new EdcException("Creating temporary key - (Warning: max 5 keys on the storage) - " + e.getMessage());
        }
    }

    @Override
    public  S3AccessKey retrieveAccessKey(String keyID) {
        try{
            return ionoss3Api.retrieveAccessKey(token, keyID);
        } catch (Exception e) {
            throw new EdcException("Retrieving temporary key: " + e.getMessage());
        }
    }
 
	@Override
	public void deleteAccessKey(String keyID) {
        try{
            ionoss3Api.deleteAccessKey(token, keyID);
        } catch (Exception e) {
            throw new EdcException("Deleting temporary key: " + e.getMessage());
        }
	}

    static String getRegion(String endpoint) {

        switch (endpoint) {
            case "https://s3-eu-central-1.ionoscloud.com":
                return "de";
            case "s3-eu-central-1.ionoscloud.com":
                return "de";
            case "https://s3-eu-central-2.ionoscloud.com":
                return "eu-central-2";
            case "s3-eu-central-2.ionoscloud.com":
                return "eu-central-2";
            case "https://s3-eu-south-2.ionoscloud.com":
                return "eu-south-2";
            case "s3-eu-south-2.ionoscloud.com":
                return "eu-south-2";
            default:
                throw new EdcException("Invalid endpoint: " + endpoint);
        }
    }

}
