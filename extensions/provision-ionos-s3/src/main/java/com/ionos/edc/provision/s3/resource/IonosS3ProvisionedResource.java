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

package com.ionos.edc.provision.s3.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.BUCKET_NAME;
import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.ENDPOINT;
import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.MAX_FILES;
import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.PATH;

@JsonDeserialize(builder = IonosS3ProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:ionoss3provisionedresource")
public class IonosS3ProvisionedResource extends ProvisionedDataDestinationResource {

    private String accessKeyID;

    public String getAccessKeyID() {
        return accessKeyID;
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

        public Builder endpoint(String endpoint) {
            dataAddressBuilder.property(ENDPOINT, endpoint);
            return this;
        }

        public Builder bucketName(String bucketName) {
            dataAddressBuilder.property(BUCKET_NAME, bucketName);
            return this;
        }

        public Builder path(String path) {
            dataAddressBuilder.property(PATH, path);
            return this;
        }

        public Builder accessKeyID(String accessKeyID) {
            provisionedResource.accessKeyID = accessKeyID;
            return this;
        }

        public Builder maxFiles(String maxFiles) {
            dataAddressBuilder.property(MAX_FILES, maxFiles);
            return this;
        }
    }
}
