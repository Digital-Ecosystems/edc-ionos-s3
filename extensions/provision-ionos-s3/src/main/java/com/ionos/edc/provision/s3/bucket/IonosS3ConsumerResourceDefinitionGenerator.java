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

import static java.util.UUID.randomUUID;

import java.util.Objects;

import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;

import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.spi.EdcException;

public class IonosS3ConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    @Override
    public ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        Objects.requireNonNull(dataRequest, "dataRequest must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        var destination = dataRequest.getDataDestination();

        var path = destination.getStringProperty(IonosBucketSchema.PATH);
        if ((path != null) && !path.endsWith("/")) {
            throw new EdcException("path must be a directory");
        }

        var id = randomUUID().toString();
        var keyName = destination.getKeyName();
        var storage = destination.getStringProperty(IonosBucketSchema.STORAGE_NAME);
        var bucketName = destination.getStringProperty(IonosBucketSchema.BUCKET_NAME);
        var accessKey = destination.getStringProperty(IonosBucketSchema.ACCESS_KEY_ID);
        var secretKey = destination.getStringProperty(IonosBucketSchema.SECRET_ACCESS_KEY);

        return IonosS3ResourceDefinition.Builder.newInstance()
                .id(id)
                .keyName(keyName)
                .storage(storage)
                .bucketName(bucketName)
                .path(path)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .build();
    }

    @Override
    public boolean canGenerate(DataRequest dataRequest, Policy policy) {
        Objects.requireNonNull(dataRequest, "dataRequest must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        return IonosBucketSchema.TYPE.equals(dataRequest.getDestinationType());
    }

}
