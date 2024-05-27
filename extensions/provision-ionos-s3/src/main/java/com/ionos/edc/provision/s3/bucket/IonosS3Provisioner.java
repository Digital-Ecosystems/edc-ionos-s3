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

package com.ionos.edc.provision.s3.bucket;

import com.ionos.edc.extension.s3.api.S3ConnectorApi;
import com.ionos.edc.extension.s3.api.S3ConnectorApiImpl;
import com.ionos.edc.extension.s3.configuration.IonosToken;

import com.ionos.edc.extension.s3.connector.ionosapi.TemporaryKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import static dev.failsafe.Failsafe.with;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.STORAGE_NAME_DEFAULT;

public class IonosS3Provisioner implements Provisioner<IonosS3ResourceDefinition, IonosS3ProvisionedResource> {

    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;
    private final S3ConnectorApi s3Api;
    private final Integer keyValidationAttempts;
    private final Long keyValidationDelay;

    public IonosS3Provisioner(Monitor monitor, RetryPolicy<Object> retryPolicy, S3ConnectorApi s3Api, int keyValidationAttempts, long keyValidationDelay) {

        this.monitor = monitor;
        this.retryPolicy = retryPolicy;
        this.s3Api = s3Api;
        this.keyValidationAttempts = keyValidationAttempts;
        this.keyValidationDelay = keyValidationDelay;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof IonosS3ResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof IonosS3ProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(IonosS3ResourceDefinition resourceDefinition,
            org.eclipse.edc.policy.model.Policy policy) {

        String bucketName = resourceDefinition.getBucketName();
        if (!s3Api.bucketExists(bucketName)) {
            createBucket(bucketName);
        }

        var temporaryKey = createTemporaryKey(resourceDefinition);

        String resourceName = resourceDefinition.getKeyName();

        var resourceBuilder = IonosS3ProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getId())
                .resourceName(resourceName)
                .bucketName(resourceDefinition.getBucketName())
                .resourceDefinitionId(resourceDefinition.getId())
                .accessKey(temporaryKey.getAccessKey())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .hasToken(true);
        if (resourceDefinition.getStorage() != null) {
            resourceBuilder = resourceBuilder.storage(resourceDefinition.getStorage());
        }
        if (resourceDefinition.getPath() != null) {
            resourceBuilder = resourceBuilder.path(resourceDefinition.getPath());
        }
        var resource = resourceBuilder.build();

        var expiryTime = OffsetDateTime.now().plusHours(1);
        var secretToken = new IonosToken(temporaryKey.getAccessKey(), temporaryKey.getSecretKey(), expiryTime.toInstant().toEpochMilli() );
        var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();

        return CompletableFuture.completedFuture(StatusResult.success(response));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            IonosS3ProvisionedResource provisionedResource, org.eclipse.edc.policy.model.Policy policy) {
        return with(retryPolicy).runAsync(() -> s3Api.deleteTemporaryKey(provisionedResource.getAccessKey()))
                .thenApply(empty ->
                        StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build())
                );
    }

    private TemporaryKey createTemporaryKey(IonosS3ResourceDefinition resourceDefinition) {
        var temporaryKey = s3Api.createTemporaryKey();

        S3ConnectorApi s3ApiTemp;
        if (resourceDefinition.getStorage() != null) {
            s3ApiTemp = new S3ConnectorApiImpl(resourceDefinition.getStorage(),
                    temporaryKey.getAccessKey(),
                    temporaryKey.getSecretKey());
        } else {
            s3ApiTemp = new S3ConnectorApiImpl(STORAGE_NAME_DEFAULT,
                    temporaryKey.getAccessKey(),
                    temporaryKey.getSecretKey());
        }

        // Validate the temporary key
        var validated = false;
        for (int i = 1; i <= keyValidationAttempts; i++) {
            if (validateKey(resourceDefinition, s3ApiTemp, i)) {
                validated = true;
                break;
            }
        }

        if (validated)
            return temporaryKey;
        else {
            // Delete the not validated temporary key
            s3Api.deleteTemporaryKey(temporaryKey.getAccessKey());
            throw new EdcException("Temporary key not validated after " + keyValidationAttempts + " attempts");
        }
    }

    private boolean validateKey(IonosS3ResourceDefinition resourceDefinition, S3ConnectorApi s3ApiTemp, int count) {
        try {
            // Wait the delay * current retry
            var delay = keyValidationDelay * count;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new EdcException("Error waiting delay to validate temporary key", e);
        }

        try {
            // Validate the key with bucket exists method
            s3ApiTemp.bucketExists(resourceDefinition.getBucketName());
            return true;
        } catch (Exception e) {
            monitor.debug("Error validating temporary key: attempts " + count + " of " + keyValidationAttempts + " - " + e.getMessage());
            return false;
        }
    }

    private void createBucket(String bucketName) {
        s3Api.createBucket(bucketName);
    }

}
