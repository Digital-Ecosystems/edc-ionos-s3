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
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;


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
        // TODO Auto-generated method stub
        return resourceDefinition instanceof IonosS3ResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        // TODO Auto-generated method stub
        return resourceDefinition instanceof IonosS3ProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(IonosS3ResourceDefinition resourceDefinition,
            org.eclipse.edc.policy.model.Policy policy) {

        String storage = resourceDefinition.getStorage();
        String bucketName = resourceDefinition.getbucketName();

      

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        // Ensure resource name is unique to avoid key collisions in local and remote
        // vaults
        if (storage == null) {
            storage = "storage";       
        }
        String resourceName = resourceDefinition.getId() + "-container";
        var resource = IonosS3ProvisionedResource.Builder.newInstance().id(bucketName).storage(storage)
                .bucketName(bucketName).resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId()).resourceName(resourceName).hasToken(true)
                .build();
        
        var response = ProvisionResponse.Builder.newInstance().resource(resource).build();
        return CompletableFuture.completedFuture(StatusResult.success(response));

    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            IonosS3ProvisionedResource provisionedResource, org.eclipse.edc.policy.model.Policy policy) {
        // TODO Auto-generated method stub
        return null;
    }

}
