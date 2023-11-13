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

import com.ionos.edc.extension.s3.api.S3ConnectorApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class IonosDataSink extends ParallelSink {
    private S3ConnectorApi s3Api;
    private String bucketName;
    private String blobName;
    
    private IonosDataSink() {

    }

    @Override
    protected StreamResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            String blobName;
            if (this.blobName != null) {
                blobName = this.blobName;
            } else {
                blobName = part.name();
            }

            try (var input = part.openStream()) {
                s3Api.uploadObject(bucketName, blobName, input);
            } catch (Exception e) {
                return uploadFailure(e, blobName);
            }
        }

        return StreamResult.success();
    }

    @NotNull
    private StreamResult<Void> uploadFailure(Exception e, String blobName) {
        var message = format("Error writing the %s object on the %s bucket: %s", blobName, bucketName, e.getMessage());
        monitor.severe(message, e);
        return StreamResult.error(message);
    }

    public static class Builder extends ParallelSink.Builder<Builder, IonosDataSink> {

        private Builder() {
            super(new IonosDataSink());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder s3Api(S3ConnectorApi s3Api) {
            sink.s3Api = s3Api;
            return this;
        }

        public Builder bucketName(String bucketName) {
            sink.bucketName = bucketName;
            return this;
        }

        public Builder blobName(String blobName) {
            sink.blobName = blobName;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.bucketName, "Bucket Name is required");
        }
    }
}
