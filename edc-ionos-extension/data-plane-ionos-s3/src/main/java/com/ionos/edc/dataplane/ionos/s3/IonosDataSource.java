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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

class IonosDataSource implements DataSource {

    private S3ConnectorApi s3Api;
    private String bucketName;
    private String blobName;
    
    private IonosDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        return success(Stream.of(new S3Part(s3Api, bucketName, blobName)));
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
            InputStream targetStream = new ByteArrayInputStream(s3Api.getFile(bucketName, blobName));
            return targetStream;
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

        public Builder bucketName(String bucketName) {
            source.bucketName = bucketName;
            return this;
        }

        public Builder client(S3ConnectorApi s3Api) {
            source.s3Api = s3Api;
            return this;
        }

        public IonosDataSource build() {
            return source;
        }

        public Builder blobName(String blobName) {
            source.blobName = blobName;
            return this;
        }
    }
}
