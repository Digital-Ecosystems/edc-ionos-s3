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

package com.ionos.edc.dataplane.ionos.s3;

import com.ionos.edc.dataplane.ionos.s3.validation.IonosSourceDataAddressValidationRule;
import com.ionos.edc.extension.s3.api.S3ConnectorApi;
import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

public class IonosDataSourceFactory implements DataSourceFactory {
    private final S3ConnectorApi s3Api;
   
    private final TypeManager typeManager;
    
    private final ValidationRule<DataAddress> validation = new IonosSourceDataAddressValidationRule();
    
    public IonosDataSourceFactory(S3ConnectorApi s3Api, TypeManager typeManager) {
        this.s3Api = s3Api;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return IonosBucketSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        return validation.apply(source);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var validationResult = validateRequest(request);
        if (validationResult.failed()) {
            throw new EdcException(String.join(", ", validationResult.getFailureMessages()));
        }
        
        var source = request.getSourceDataAddress();
       
        return IonosDataSource.Builder.newInstance().client(s3Api)
                .bucketName(source.getStringProperty(IonosBucketSchema.BUCKET_NAME))
                .blobName(source.getStringProperty(IonosBucketSchema.BLOB_NAME))
                .build();
    }

}
