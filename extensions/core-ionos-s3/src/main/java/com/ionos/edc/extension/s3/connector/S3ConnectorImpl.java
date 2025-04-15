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
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.eclipse.edc.spi.EdcException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class S3ConnectorImpl implements S3Connector {

    private static final long ENDPOINTS_CACHE_TTL = 3600000; // 1 Hour

    private final S3ApiClient S3ApiClient = new S3ApiClient();

    private String defaultRegionId;
    private final String accessKey;
    private final String secretKey;
    private String token;
    private final int maxFiles;

    private final PassiveExpiringMap<String, String> endpointsCache = new PassiveExpiringMap<>(ENDPOINTS_CACHE_TTL);

    public S3ConnectorImpl(String defaultRegionId, String accessKey, String secretKey, String token, int maxFiles) {
        this.defaultRegionId = defaultRegionId;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.token = token;
        this.maxFiles = maxFiles;
    }

    public S3ConnectorImpl(String accessKey, String secretKey, int maxFiles) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.maxFiles = maxFiles;
    }

    private MinioClient buildClientByRegion(String regionId) {

        String endpoint;
        if (regionId != null) {
            endpoint = getEndpoint(regionId);
        } else {
            endpoint = getEndpoint(defaultRegionId);
        }

        return MinioClient.builder().
                endpoint(endpoint).
                credentials(accessKey, secretKey).
                build();
    }

    private MinioClient buildClientByEndpoint(String endpoint) {

        return MinioClient.builder().
                endpoint(endpoint).
                credentials(accessKey, secretKey).
                build();
    }

    @Override
    public String getDefaultRegionId() {
        return defaultRegionId;
    }

    @Override
    public int getMaxFiles() {
        return maxFiles;
    }

    @Override
    public String getEndpoint(String regionId) {

        if (endpointsCache.containsKey(regionId))
            return endpointsCache.get(regionId);

        var regions = S3ApiClient.retrieveRegions(token);

        for (S3Region region: regions.getItems()) {
            if (region.getId().equals(regionId)) {
                var endpoint = "https://" + region.getProperties().getEndpoint();
                endpointsCache.put(regionId, endpoint);
                return endpoint;
            }
        }
        throw new EdcException("Invalid region: " + regionId);
    }

    @Override
    public boolean bucketExists(String bucketName, String regionId) {

        try (var minioClient = buildClientByRegion(regionId)) {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName.toLowerCase())
                    .build());

        } catch (Exception e) {
            throw new EdcException(format("Error verifying if bucket %s exists in region %s", bucketName, regionId), e);
        }
    }

    @Override
    public void createBucket(String bucketName, String regionId) {

        try (var minioClient = buildClientByRegion(regionId)) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName.toLowerCase())
                    .build());
        } catch (Exception e) {
            throw new EdcException(format("Error creating bucket %s in region %s", bucketName, regionId), e);
        }
    }

    @Override
    public void uploadObject(String bucketName, String endpoint, String objectName, ByteArrayInputStream stream) {

        try (var minioClient = buildClientByEndpoint(endpoint)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName.toLowerCase())
                    .object(objectName)
                    .stream(stream, stream.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new EdcException(format("Error uploading object %s in bucket %s, endpoint %s", objectName, bucketName, endpoint), e);
        }
    }

    @Override
    public ByteArrayInputStream getObject(String bucketName, String regionId, String objectName) {
        var request = GetObjectArgs.builder()
                .bucket(bucketName.toLowerCase())
                .object(objectName)
                .build();

        try (var minioClient = buildClientByRegion(regionId); var response = minioClient.getObject(request)) {
            return new ByteArrayInputStream(response.readAllBytes());
        } catch (Exception e) {
            throw new EdcException(format("Error getting object %s in bucket %s, region %s", objectName, bucketName, regionId), e);
        }
    }

    @Override
    public ByteArrayInputStream getObject(String bucketName, String regionId, String objectName, long offset, long length) {
        var request = GetObjectArgs.builder()
                .bucket(bucketName.toLowerCase())
                .object(objectName)
                .offset(offset)
                .length(length)
                .build();

        try (var minioClient = buildClientByRegion(regionId); var response = minioClient.getObject(request)) {
            return new ByteArrayInputStream(response.readAllBytes());
        } catch (Exception e) {
            throw new EdcException(format("Error getting object %s in bucket %s, region %s", objectName, bucketName, regionId), e);
        }
    }

    @Override
    public List<S3Object> listObjects(String bucketName, String regionId, String objectName) {

        try (var minioClient = buildClientByRegion(regionId)) {
            var objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName.toLowerCase())
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

        } catch (Exception e) {
            throw new EdcException(format("Error listing objects with prefix %s in bucket %s, region %s", objectName, bucketName, regionId), e);
        }
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
}
