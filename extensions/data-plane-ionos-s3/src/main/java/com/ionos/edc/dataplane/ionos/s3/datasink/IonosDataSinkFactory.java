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

package com.ionos.edc.dataplane.ionos.s3.datasink;

import com.ionos.edc.dataplane.ionos.s3.validators.IonosDataSinkDestinationValidator;
import com.ionos.edc.extension.s3.connector.S3ConnectorImpl;
import com.ionos.edc.extension.s3.types.IonosToken;
import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.MAX_FILES;

public class IonosDataSinkFactory implements DataSinkFactory {

    private final ExecutorService executorService;
    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;

    private final Validator<DataAddress> destinationValidator = new IonosDataSinkDestinationValidator();

    public IonosDataSinkFactory(ExecutorService executorService, Monitor monitor, Vault vault, TypeManager typeManager) {
        this.executorService = executorService;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
    }

    @Override
    public String supportedType() {
        return IonosBucketSchema.TYPE;
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        var destination = request.getDestinationDataAddress();
        return destinationValidator.validate(destination).flatMap(ValidationResult::toResult);
    }

    @Override
    public DataSink createSink(DataFlowStartMessage request) {
        var destination = request.getDestinationDataAddress();

        var secret = vault.resolveSecret(request.getDestinationDataAddress().getKeyName());
        if (secret == null) {
            throw new EdcException("Missing destination temporary token");
        }
        var token = typeManager.readValue(secret, IonosToken.class);

        var endpoint = destination.getStringProperty(IonosBucketSchema.ENDPOINT);
        var maxFiles = Integer.parseInt(destination.getStringProperty(MAX_FILES));

        var s3Connector = new S3ConnectorImpl(token.getAccessKey(), token.getSecretKey(), maxFiles);

        return IonosDataSink.Builder.newInstance()
                .endpoint(endpoint)
                .bucketName(destination.getStringProperty(IonosBucketSchema.BUCKET_NAME))
                .path(destination.getStringProperty(IonosBucketSchema.PATH))
                .requestId(request.getId())
                .executorService(executorService)
                .monitor(monitor)
                .s3Connector(s3Connector)
                .build();
    }
}

