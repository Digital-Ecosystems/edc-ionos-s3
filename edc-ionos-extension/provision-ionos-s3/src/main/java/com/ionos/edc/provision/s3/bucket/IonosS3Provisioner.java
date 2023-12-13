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

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import static dev.failsafe.Failsafe.with;

public class IonosS3Provisioner implements Provisioner<IonosS3ResourceDefinition, IonosS3ProvisionedResource> {
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final S3ConnectorApi s3Api;

    public IonosS3Provisioner(RetryPolicy<Object> retryPolicy, Monitor monitor, S3ConnectorApi s3Api) {

        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.s3Api = s3Api;
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

        var serviceAccount = s3Api.createTemporaryKey();

        String resourceName = resourceDefinition.getKeyName();

        var resourceBuilder = IonosS3ProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getId())
                .resourceName(resourceName)
                .bucketName(resourceDefinition.getBucketName())
                .resourceDefinitionId(resourceDefinition.getId())
                .accessKey(serviceAccount.getAccessKey())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .hasToken(true);
        if (resourceDefinition.getStorage() != null) {
            resourceBuilder = resourceBuilder.storage(resourceDefinition.getStorage());
        }
        if (resourceDefinition.getBlobName() != null) {
            resourceBuilder = resourceBuilder.blobName(resourceDefinition.getBlobName());
        }
        var resource = resourceBuilder.build();

        var expiryTime = OffsetDateTime.now().plusHours(1);
        var secretToken = new IonosToken(serviceAccount.getAccessKey(), serviceAccount.getSecretKey(), expiryTime.toInstant().toEpochMilli() );
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
    
    @NotNull
    private CompletableFuture<Void> createBucket(String bucketName) {
        return with(retryPolicy).runAsync(() -> {
            s3Api.createBucket(bucketName);
        });
    }

}
