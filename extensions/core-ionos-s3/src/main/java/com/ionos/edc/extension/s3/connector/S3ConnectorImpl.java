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

package com.ionos.edc.extension.s3.connector;

import com.ionos.edc.extension.s3.api.S3AccessKey;
import com.ionos.edc.extension.s3.api.S3ApiClient;

import com.ionos.edc.extension.s3.api.S3Region;
import com.ionos.edc.extension.s3.types.S3Object;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.REGION_ID_DEFAULT;

public class S3ConnectorImpl implements S3Connector {

    private final S3ApiClient S3ApiClient = new S3ApiClient();

    private final MinioClient minioClient;
    private final String regionId;
    private final String token;
    private final int maxFiles;

    public S3ConnectorImpl(String regionId, @NotNull String accessKey, @NotNull String secretKey, @NotNull String token, int maxFiles) {
        this.token = token;
        this.maxFiles = maxFiles;

        this.regionId = Objects.requireNonNullElse(regionId, REGION_ID_DEFAULT);
        var endpoint = getEndpoint( this.regionId , token);

        this.minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    @Override
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName.toLowerCase())) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName.toLowerCase())
                        .region(regionId)
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
                    .region(regionId)
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
                    .region(regionId)
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
                .region(regionId)
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
                .region(regionId)
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
                .region(regionId)
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
            return S3ApiClient.createAccessKey(token);
        } catch (Exception e) {
            throw new EdcException("Error creating access key", e);
        }
    }

    @Override
    public  S3AccessKey retrieveAccessKey(String keyID) {
        try{
            return S3ApiClient.retrieveAccessKey(token, keyID);
        } catch (Exception e) {
            throw new EdcException("Error retrieving access key", e);
        }
    }
 
	@Override
	public void deleteAccessKey(String keyID) {
        try{
            S3ApiClient.deleteAccessKey(token, keyID);
        } catch (Exception e) {
            throw new EdcException("Error deleting access key", e);
        }
	}

    private String getEndpoint(String regionId, String token) {
        var regions = S3ApiClient.retrieveRegions(token);

        for (S3Region region: regions.getItems()) {
            if (region.getId().equals(regionId)) {
                return "https://" + region.getProperties().getEndpoint();
            }
        }
        throw new EdcException("Invalid region: " + regionId);
    }

    @Override
    public S3Connector clone(String region, String accessKey, String secretKey) {
        return new S3ConnectorImpl(region, accessKey, secretKey, this.token, this.maxFiles);
    }
}
