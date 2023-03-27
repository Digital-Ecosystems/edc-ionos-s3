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
//    	monitor.debug("provisionIOnos 62");
    	
        String storage = resourceDefinition.getStorage();
        String bucketName = resourceDefinition.getbucketName();
        monitor.debug(" storage " + storage +" bucketName " + bucketName  );
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        if (storage == null) {
            storage = "storage";       
        }
//        monitor.debug(" vou tentar");
        return with(retryPolicy).getAsync(() -> s3Api.bucketExists(bucketName))
        		.thenCompose(exists -> {
                    if (exists) {
                        return getExistBucket(bucketName);
                    } else {
                        return createBucket(bucketName);
                    }
                })
                .thenApply(writeOnlySas -> {
                    // Ensure resource name is unique to avoid key collisions in local and remote vaults
                	String resourceName = resourceDefinition.getId() + "-container";
                	 monitor.debug("ResourceName or keyname" +  resourceDefinition.getbucketName());
                	 var resource = IonosS3ProvisionedResource.Builder.newInstance().id( resourceDefinition.getbucketName())  
                			 .storage(resourceDefinition.getStorage())
                             .bucketName(resourceDefinition.getbucketName()).resourceDefinitionId(resourceDefinition.getId())
                             .transferProcessId(resourceDefinition.getTransferProcessId()).resourceName(resourceName).hasToken(true)
                             .build();
                	 
                	var serviceAccount =s3Api.createTemporaryKey();
                	var secretToken = new IonosToken(serviceAccount.getAccessKey(), serviceAccount.getSecretKey(), expiryTime.toInstant().toEpochMilli() );
//                	monitor.debug("token: " + secretToken);
                    var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
                   
                    return StatusResult.success(response);
                });
    
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            IonosS3ProvisionedResource provisionedResource, org.eclipse.edc.policy.model.Policy policy) {
    	 monitor.debug("deprovisionIOnos 106");
    	 return with(retryPolicy).runAsync(() -> s3Api.deleteBucket(provisionedResource.getBucketName()))
                  .thenApply(empty -> StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build()));

      
    }
    
    @NotNull
    private CompletableFuture<Void> getExistBucket(String bucketName) {
        monitor.debug("Provisioner: re-use existing bucket " + bucketName);
        return CompletableFuture.completedFuture(null);
    }
    
    @NotNull
    private CompletableFuture<Void> createBucket(String bucketName) {
        return with(retryPolicy)
                .runAsync(() -> {
                    s3Api.createBucket(bucketName);
                    monitor.debug("Provisioner: created a new bucket " + bucketName);
                });
    }


}
