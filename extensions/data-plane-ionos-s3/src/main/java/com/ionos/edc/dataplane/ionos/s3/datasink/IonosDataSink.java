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

import com.ionos.edc.dataplane.ionos.s3.datasource.IonosDataSource;
import com.ionos.edc.extension.s3.connector.S3Connector;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class IonosDataSink extends ParallelSink {

    private S3Connector s3Connector;
    private String endpoint;
    private String bucketName;
    private String path;

    private IonosDataSink() {}

    @Override
    protected StreamResult<Object> transferParts(List<DataSource.Part> parts) {

        for (DataSource.Part part : parts) {
            if (StringUtils.isNullOrBlank(part.name())) {
                throw new EdcException(format("Transfer dataSource [%s] is not returning a name and it is required to transfer to a S3 bucket!", part.getClass().getName()));
            }

            String blobName = (this.path != null) ? this.path + part.name() : part.name();

            ByteArrayOutputStream streamsOutput = null;
            InputStream stream = null;
            try {
                streamsOutput = new ByteArrayOutputStream();
                stream = part.openStream();

                // TODO Make this more configurable
                if (part instanceof IonosDataSource.S3Part) {
                    // Multiple fetches
                    while (stream != null) {
                        try {
                            streamsOutput.write(stream.readAllBytes());
                            stream.close();

                        } catch (Exception e) {
                            return uploadFailure(e, blobName);
                        }

                        stream = part.openStream();
                    }
                } else {
                    // Single fetch
                    try {
                        streamsOutput.write(stream.readAllBytes());
                        stream.close();

                    } catch (Exception e) {
                        return uploadFailure(e, blobName);
                    }
                }

                var byteArray = streamsOutput.toByteArray();
                try (var streamsInput = new ByteArrayInputStream(byteArray)) {
                    s3Connector.uploadObject(bucketName, endpoint, blobName, streamsInput);
                    streamsOutput.close();

                } catch (Exception e) {
                    return uploadFailure(e, blobName);
                }
            } finally {
                try {
                    if (streamsOutput != null) {
                        streamsOutput.close();
                    }
                    if (stream != null) {
                        stream.close();
                    }

                } catch (Exception e) {
                    monitor.severe("Error closing streams", e);
                }
            }
        }

        return StreamResult.success();
    }

    @NotNull
    private StreamResult<Object> uploadFailure(Exception e, String blobName) {
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

        public Builder s3Connector(S3Connector s3Connector) {
            sink.s3Connector = s3Connector;
            return this;
        }

        public Builder endpoint(String endpoint) {
            sink.endpoint = endpoint;
            return this;
        }

        public Builder bucketName(String bucketName) {
            sink.bucketName = bucketName;
            return this;
        }

        public Builder path(String path) {
            sink.path = path;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.bucketName, "Bucket Name is required");
            Objects.requireNonNull(sink.bucketName, "Endpoint is required");
        }
    }
}
