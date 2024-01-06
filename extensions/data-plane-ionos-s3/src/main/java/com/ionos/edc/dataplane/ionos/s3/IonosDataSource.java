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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

class IonosDataSource implements DataSource {

    private S3ConnectorApi s3Api;
    private String bucketName;
    private String blobName;
    private static Monitor monitor;

    private IonosDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        return success(Stream.of(new S3Part(s3Api, bucketName, blobName)));
    }

    @Override
    public void close() {
    }

    private static class S3Part implements Part {
        private final S3ConnectorApi s3Api;
        private final String bucketName;
        private final String blobName;

        S3Part(S3ConnectorApi s3Api, String bucketName, String blobName) {
            super();
            this.s3Api = s3Api;
            this.bucketName = bucketName;
            this.blobName = blobName;
        }

        @Override
        public String name() {
            return blobName;
        }

        @Override
        public InputStream openStream() {
            try {
                byte[] file = s3Api.getFile(bucketName, blobName);
                if (file == null) {
                    throw new EdcException("Error trying to getFile");
                }

                InputStream targetStream = new ByteArrayInputStream(file);
                return targetStream;
            } catch (Exception e) {
                openingFailure(e, blobName);
            }
            return null;
        }

        @NotNull
        private StreamResult<Void> openingFailure(Exception e, String blobName) {
            var message = format("Error opening file %s: %s", blobName, e.getMessage());
            monitor.severe(message, e);
            return StreamResult.error(message);
        }
    }



    public static class Builder {
        private final IonosDataSource source;

        private Builder() {
            source = new IonosDataSource();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public IonosDataSource build() {
            return source;
        }

        public Builder client(S3ConnectorApi s3Api) {
            source.s3Api = s3Api;
            return this;
        }

        public Builder bucketName(String bucketName) {
            source.bucketName = bucketName;
            return this;
        }

        public Builder blobName(String blobName) {
            source.blobName = blobName;
            return this;
        }
    }
}
