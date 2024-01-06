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

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.eclipse.edc.spi.EdcException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class S3ConnectorApiImpl implements S3ConnectorApi {

    MinioConnector minConnector = new MinioConnector();
    HttpConnector ionosApi = new HttpConnector();

    private MinioClient minioClient;
    private final String region;
    private final String token;


    public S3ConnectorApiImpl(String endpoint, String accessKey, String secretKey, String token) {
        if (accessKey != null && secretKey != null && endpoint != null)
            this.minioClient = minConnector.connect(endpoint, accessKey, secretKey);
        this.region = getRegion(endpoint);
        this.token = token;
    }

    @Override
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName.toLowerCase())) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName.toLowerCase()).region(region).build());
            } catch (Exception e) {
                throw new EdcException("Creating bucket: " + e.getMessage());
            }
        }
    }

    @Override
    public void uploadParts(String bucketName, String fileName, ByteArrayInputStream  part) {
        if (!bucketExists(bucketName.toLowerCase())) {
            createBucket(bucketName.toLowerCase());
        }
       
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName.toLowerCase()).region(region).object(fileName).stream(part, part.available(), -1).build());
        } catch (Exception e) {
            throw new EdcException("Uploading parts: " + e.getMessage());
        }
    }

    @Override
    public byte[] getFile(String bucketName, String fileName) {
        if (!bucketExists(bucketName.toLowerCase())) {
            throw new EdcException("Bucket not found - " + bucketName);
        }

        InputStream stream;
        try {
            stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName.toLowerCase()).region(region).object(fileName).build());
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new EdcException("Getting file - " + e.getMessage());
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName.toLowerCase()).region(this.region).build());
        } catch (Exception e) {
            throw new EdcException("Verifying if bucket exists - " + e.getMessage());
        }
    }
    
    @Override
    public  TemporaryKey createTemporaryKey() {
		try{
            return ionosApi.createTemporaryKey(token);
        } catch (Exception e) {
            throw new EdcException("Creating temporary key - (Warning: max 5 keys on the storage) - " + e.getMessage());
        }
    }
 
	@Override
	public void deleteTemporaryKey(String accessKey) {
        try{
            ionosApi.deleteTemporaryAccount(token,accessKey);
        } catch (Exception e) {
            throw new EdcException("Deleting temporary key: " + e.getMessage());
        }
	}

    private String getRegion(String endpoint) {
        if (!endpoint.contains(".ionoscloud.com"))
            return endpoint;

        var region = endpoint.substring(0, endpoint.indexOf(".ionoscloud.com"));

        if (region.contains("https://" )) {
            return region.substring(region.indexOf("https://") + 8);
        } else if (region.contains("http://" )) {
            return region.substring(region.indexOf("http://") + 7);
        } else {
            return region;
        }
    }

}
