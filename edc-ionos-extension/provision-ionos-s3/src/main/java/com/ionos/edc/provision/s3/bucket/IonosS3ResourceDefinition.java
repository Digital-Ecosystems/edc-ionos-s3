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

import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;

import java.util.Objects;

public class IonosS3ResourceDefinition extends ResourceDefinition {

    private String keyName;
    private String storage;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String blobName;

    public IonosS3ResourceDefinition() {
        super();
    }

    public String getKeyName() {
        return keyName;
    }
    public String getStorage() {
        return storage;
    }
    public String getAccessKey() {
		return accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public String getBucketName() {
        return bucketName;
    }
    public String getBlobName() {
        return blobName;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder()).keyName(keyName).storage(storage).accessKey(accessKey).secretKey(secretKey).bucketName(bucketName);
    }

    public static class Builder extends ResourceDefinition.Builder<IonosS3ResourceDefinition, Builder> {

        private Builder() {
            super(new IonosS3ResourceDefinition());
        }

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
        
        public Builder accessKey(String accessKey) {
            resourceDefinition.accessKey = accessKey;
            return this;
        }
        
        public Builder secretKey(String secretKey) {
            resourceDefinition.secretKey = secretKey;
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

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.keyName, "Key Name is required");
            Objects.requireNonNull(resourceDefinition.bucketName, "Bucket Name is required");
        }
    }
}
