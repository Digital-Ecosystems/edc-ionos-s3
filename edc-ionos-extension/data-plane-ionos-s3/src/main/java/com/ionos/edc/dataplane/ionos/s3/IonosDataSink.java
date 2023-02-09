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
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.lang.String.format;

public class IonosDataSink extends ParallelSink {
    private String accountName;
    private String containerName;
    private S3ConnectorApi s3Api;
    private String bucketName;
    private String keyName;

    private IonosDataSink() {

    }

    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {

        for (DataSource.Part part : parts) {
            try (var input = part.openStream()) {
                String blobName = part.name();
                s3Api.uploadParts(bucketName, blobName, new ByteArrayInputStream(input.readAllBytes()));
            } catch (Exception e) {
                return uploadFailure(e, keyName);
            }
        }

        return StatusResult.success();
    }

    @NotNull
    private StatusResult<Void> uploadFailure(Exception e, String keyName) {
        var message = format("Error writing the %s object on the %s bucket: %s", keyName, bucketName, e.getMessage());
        monitor.severe(message, e);
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, message);
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

        public Builder keyName(String keyName) {
            sink.keyName = keyName;
            return this;
        }

        @Override
        protected void validate() {
        }
    }
}
