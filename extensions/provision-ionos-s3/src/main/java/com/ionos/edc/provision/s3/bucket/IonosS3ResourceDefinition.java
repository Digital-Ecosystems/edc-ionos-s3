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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;

@JsonDeserialize(as=IonosS3ResourceDefinition.class)
public class IonosS3ResourceDefinition extends ResourceDefinition {

    private String keyName;
    private String regionId;
    private String bucketName;
    private String path;
    private String accessKey;
    private String secretKey;

    public IonosS3ResourceDefinition() {

    }
    public String getKeyName() {
        return keyName;
    }
    public String getRegionId() {
        return regionId;
    }
	public String getBucketName() {
        return bucketName;
    }
    public String getPath() {
        return path;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .keyName(keyName)
                .regionId(regionId)
                .bucketName(bucketName)
                .path(path)
                .accessKey(accessKey)
                .secretKey(secretKey);
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
        public Builder regionId(String regionId) {
            resourceDefinition.regionId = regionId;
            return this;
        }
        public Builder bucketName(String bucketName) {
            resourceDefinition.bucketName = bucketName;
            return this;
        }
        public Builder path(String path) {
            resourceDefinition.path = path;
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

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.bucketName, "Bucket Name is required");
        }
    }
}
