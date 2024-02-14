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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;

@JsonDeserialize(as=IonosS3ResourceDefinition.class)
public class IonosS3ResourceDefinition extends ResourceDefinition {
    private String keyName;
    private String storage;

    private String bucketName;
    private String blobName;
    private String accessKey = "DEFAULT";


    public IonosS3ResourceDefinition() {

    }
    public String getKeyName() {
        return keyName;
    }
    public String getStorage() {
        return storage;
    }

	public String getBucketName() {
        return bucketName;
    }
    public String getBlobName() {
        return blobName;
    }
    public String getAccessKey() {
        return accessKey;
    }
    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .keyName(keyName)
                .storage(storage)
                .accessKey(accessKey)
                .bucketName(bucketName)
                .blobName(blobName);
    }

    public static class Builder extends ResourceDefinition.Builder<IonosS3ResourceDefinition, Builder> {

        private Builder() {

            super(new IonosS3ResourceDefinition());
        }
        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder keyName(String keyName) {
            resourceDefinition.keyName = keyName;
            return this;
        }

        public Builder storage(String storage) {
            resourceDefinition.storage = storage;
            return this;
        }

        
        public Builder bucketName(String bucketName) {
            resourceDefinition.bucketName = bucketName;
            return this;
        }

        public Builder blobName(String blobName) {
            resourceDefinition.blobName = blobName;
            return this;
        }
        public Builder accessKey(String accessKey) {
            resourceDefinition.accessKey = accessKey;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
           // Objects.requireNonNull(resourceDefinition.keyName, "Key Name is required");
            Objects.requireNonNull(resourceDefinition.bucketName, "Bucket Name is required");
        }
    }
}
