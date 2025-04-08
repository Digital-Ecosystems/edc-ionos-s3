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

package com.ionos.edc.dataplane.ionos.s3.datasource;

import com.ionos.edc.extension.s3.connector.S3Connector;
import com.ionos.edc.extension.s3.schema.IonosBucketSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

public class IonosDataSourceFactory implements DataSourceFactory {

    private final S3Connector s3Connector;
    
    public IonosDataSourceFactory(S3Connector s3Connector) {
        this.s3Connector = s3Connector;
    }

    @Override
    public String supportedType() {
        return IonosBucketSchema.TYPE;
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        return Result.success();
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        var source = request.getSourceDataAddress();

        return IonosDataSource.Builder.newInstance()
                .client(s3Connector)
                .regionId(source.getStringProperty(IonosBucketSchema.REGION_ID))
                .bucketName(source.getStringProperty(IonosBucketSchema.BUCKET_NAME))
                .blobName(source.getStringProperty(IonosBucketSchema.BLOB_NAME))
                .filterIncludes(source.getStringProperty(IonosBucketSchema.FILTER_INCLUDES))
                .filterExcludes(source.getStringProperty(IonosBucketSchema.FILTER_EXCLUDES))
                .build();
    }

}
