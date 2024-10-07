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
import com.ionos.edc.extension.s3.configuration.IonosToken;

import com.ionos.edc.extension.s3.connector.ionosapi.S3AccessKey;
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

        var temporaryKey = createTemporaryKey();

        String resourceName = resourceDefinition.getKeyName();

        var resourceBuilder = IonosS3ProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getId())
                .resourceName(resourceName)
                .bucketName(resourceDefinition.getBucketName())
                .resourceDefinitionId(resourceDefinition.getId())
                .accessKeyID(temporaryKey.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .hasToken(true);
        if (resourceDefinition.getRegionId() != null) {
            resourceBuilder = resourceBuilder.regionId(resourceDefinition.getRegionId());
        }
        if (resourceDefinition.getPath() != null) {
            resourceBuilder = resourceBuilder.path(resourceDefinition.getPath());
        }
        var resource = resourceBuilder.build();

        var expiryTime = OffsetDateTime.now().plusHours(1);
        var secretToken = new IonosToken(temporaryKey.getProperties().getAccessKey(),
                temporaryKey.getProperties().getSecretKey(),
                expiryTime.toInstant().toEpochMilli());
        var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();

        return CompletableFuture.completedFuture(StatusResult.success(response));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            IonosS3ProvisionedResource provisionedResource, org.eclipse.edc.policy.model.Policy policy) {
        return with(retryPolicy).runAsync(() -> s3Api.deleteAccessKey(provisionedResource.getAccessKeyID()))
                .thenApply(empty ->
                        StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build())
                );
    }

    private S3AccessKey createTemporaryKey() {
        var accessKey = s3Api.createAccessKey();

        // Validate the temporary key
        var validated = false;
        int attempts = 0;
        while(attempts <= keyValidationAttempts) {
            attempts++;
            if (validateKey(accessKey)) {
                validated = true;
                break;
            }
        }

        if (validated) {
            monitor.debug("[IonosS3Provisioner] Temporary key validated after " + attempts + " attempts of " + keyValidationDelay + " ms");
            return accessKey;
        } else {
            // Delete the not validated temporary key
            s3Api.deleteAccessKey(accessKey.getId());
            throw new EdcException("Temporary key not validated after " + attempts + " attempts of " + keyValidationDelay + " ms");
        }
    }

    private boolean validateKey(S3AccessKey accessKey) {
        try {
            // Wait the validation delay
            Thread.sleep(keyValidationDelay);
        } catch (InterruptedException e) {
            throw new EdcException("Error waiting delay to validate temporary key", e);
        }

        // Validate the key status
        var retrievedAccessKey = s3Api.retrieveAccessKey(accessKey.getId());
        return (retrievedAccessKey.getMetadata().getStatus().equals(S3AccessKey.AVAILABLE_STATUS));
    }

    private void createBucket(String bucketName) {
        s3Api.createBucket(bucketName);
    }

}
