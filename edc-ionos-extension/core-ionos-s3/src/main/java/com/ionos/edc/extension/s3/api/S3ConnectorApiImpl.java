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
import com.ionos.edc.extension.s3.connector.ionosapi.HttpConnector;
import com.ionos.edc.extension.s3.connector.ionosapi.TemporaryKey;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class S3ConnectorApiImpl implements S3ConnectorApi {

    private final MinioConnector minConnector = new MinioConnector();
    private final HttpConnector ionosApi = new HttpConnector();
    private MinioClient minioClient;
    private String token;
    private final Integer maxFiles;

    public S3ConnectorApiImpl(String endpoint, String accessKey, String secretKey, int maxFiles) {
        if(accessKey != null && secretKey  != null && endpoint != null)
            this.minioClient = minConnector.connect(endpoint, accessKey, secretKey);
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
                        .build());
            } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException |
                    InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException |
                    IllegalArgumentException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void uploadObject(String bucketName, String objectName, InputStream  stream) {

        if (!bucketExists(bucketName.toLowerCase())){
            createBucket(bucketName.toLowerCase());
        }

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(stream, stream.available(), stream.available())
                    .build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException |
                 IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {

        if (!bucketExists(bucketName.toLowerCase())) {
            return null;
        }

        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());

        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException |
                InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException |
                IllegalArgumentException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<S3Object> listObjects(String bucketName, String objectName) {

        var objects = minioClient.listObjects(ListObjectsArgs.builder()
            .bucket(bucketName.toLowerCase())
            .prefix(objectName)
            .recursive(true)
            .maxKeys(maxFiles)
            .build());

        List<S3Object> objectsName = StreamSupport
                .stream(objects.spliterator(), false)
                .map(item -> {
                    try {
                        return item.get();
                    } catch (Exception e) {
                        throw new EdcException("Error fetching object", e);
                    }
                })
                .map(item -> new S3Object(item.objectName(), item.size()))
                .collect(Collectors.toList());

        return objectsName;
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName.toLowerCase()).build());

        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException |
                InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException |
                IllegalArgumentException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public  TemporaryKey createTemporaryKey() {
		return ionosApi.createTemporaryKey(token);
    }
 
	@Override
	public void deleteTemporaryKey(String accessKey) {
		ionosApi.deleteTemporaryAccount(token,accessKey);
	}

}
