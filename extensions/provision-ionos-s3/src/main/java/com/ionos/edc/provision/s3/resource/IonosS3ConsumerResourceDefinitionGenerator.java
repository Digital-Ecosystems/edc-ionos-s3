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

import static java.util.UUID.randomUUID;

import java.util.Objects;

import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;

import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.Nullable;

public class IonosS3ConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess transferProcess, Policy policy) {
        Objects.requireNonNull(transferProcess, "transferProcess must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        var destination = transferProcess.getDataDestination();
        Objects.requireNonNull(destination, "dataDestination must always be provided");

        var path = destination.getStringProperty(IonosBucketSchema.PATH);
        if ((path != null) && !path.endsWith("/")) {
            throw new EdcException("path must be a directory");
        }

        var id = randomUUID().toString();

        return IonosS3ResourceDefinition.Builder.newInstance()
                .id(id)
                .keyName(destination.getKeyName())
                .regionId(destination.getStringProperty(IonosBucketSchema.REGION_ID))
                .bucketName(destination.getStringProperty(IonosBucketSchema.BUCKET_NAME))
                .path(path)
                .build();
    }

    @Override
    public boolean canGenerate(TransferProcess transferProcess, Policy policy) {
        Objects.requireNonNull(transferProcess, "transferProcess must always be provided");
        Objects.requireNonNull(policy, "policy must always be provided");

        return IonosBucketSchema.PUSH_TRANSFER_TYPE.equals(transferProcess.getTransferType());
    }

}
