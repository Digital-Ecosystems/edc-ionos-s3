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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.*;

@JsonDeserialize(builder = IonosS3ProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:ionoss3provisionedresource")
public class IonosS3ProvisionedResource extends ProvisionedDataDestinationResource {

    private String accessKey;

    public String getStorage() {
        return getDataAddress().getStringProperty(STORAGE_NAME);
    }
    public String getBucketName() {
        return getDataAddress().getStringProperty(BUCKET_NAME);
    }
    public String getBlobName() {
        return getDataAddress().getStringProperty(BLOB_NAME);
    }
    public String getAccessKey() {
        return accessKey;
    }
 
    private IonosS3ProvisionedResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder
            extends ProvisionedDataDestinationResource.Builder<IonosS3ProvisionedResource, Builder> {

        private Builder() {
            super(new IonosS3ProvisionedResource());
            dataAddressBuilder.type(IonosBucketSchema.TYPE);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder storage(String storage) {
            dataAddressBuilder.property(STORAGE_NAME, storage);
            return this;
        }

        public Builder bucketName(String bucketName) {
            dataAddressBuilder.property(BUCKET_NAME, bucketName);
            return this;
        }

        public Builder blobName(String blobName) {
            dataAddressBuilder.property(BLOB_NAME, blobName);
            return this;
        }

        public Builder accessKey(String accessKey) {
            provisionedResource.accessKey = accessKey;
            return this;
        }
    }
}
