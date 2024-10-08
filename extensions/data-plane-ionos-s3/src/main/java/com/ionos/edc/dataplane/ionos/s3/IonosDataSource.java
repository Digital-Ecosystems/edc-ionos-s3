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

import com.ionos.edc.dataplane.ionos.s3.util.FileTransferHelper;
import com.ionos.edc.extension.s3.connector.S3Connector;
import com.ionos.edc.extension.s3.types.S3Object;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;

class IonosDataSource implements DataSource {

    private S3Connector s3Connector;
    private Monitor monitor;
    private String bucketName;
    private String blobName;
    private Pattern filterIncludes;
    private Pattern filterExcludes;

    private IonosDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {

        var objects = s3Connector.listObjects(this.bucketName, this.blobName);

        if (objects.isEmpty()) {
            return failure(new StreamFailure(
                    List.of("No files found in bucket " + bucketName + " with blobName " + blobName), NOT_FOUND)
            );
        }

        if (this.filterIncludes != null) {
            objects = objects.stream()
                    .filter(object -> applyFilterIncludes(object))
                    .collect(Collectors.toList());
        }

        if (this.filterExcludes != null) {
            objects = objects.stream()
                    .filter(object -> applyFilterExcludes(object))
                    .collect(Collectors.toList());
        }

        if (objects.isEmpty()) {
            return failure(new StreamFailure(
                    List.of("No files found in bucket " + bucketName + " with blobName " + blobName), NOT_FOUND)
            );
        }

        List<Part> parts = objects.stream()
                .map(object -> new S3Part(s3Connector, monitor, bucketName, object.objectName(), object.isDirectory(), object.size()))
                .collect(Collectors.toList());
        return success(parts.stream());
    }

    boolean applyFilterIncludes(S3Object object) {
        if (object.isRootObject(blobName))
            return true;

        return filterIncludes.matcher(object.shortObjectName(blobName)).matches();
    }
    boolean applyFilterExcludes(S3Object object) {
        if (object.isRootObject(blobName))
            return true;

        return !filterExcludes.matcher(object.shortObjectName(blobName)).matches();
    }

    @Override
    public void close() {
    }

    public static class S3Part implements Part {

        private final S3Connector s3Connector;
        private final Monitor monitor;
        private final String bucketName;
        private final String blobName;
        private final boolean isDirectory;
        private final long fileSize;

        private boolean isOpened = true;
        private long currentOffset = 0;

        S3Part(S3Connector s3Connector, Monitor monitor, String bucketName, String blobName, boolean isDirectory, long fileSize) {
            super();
            this.s3Connector = s3Connector;
            this.monitor = monitor;
            this.bucketName = bucketName;
            this.blobName = blobName;
            this.isDirectory = isDirectory;
            this.fileSize = fileSize;
        }

        @Override
        public String name() {
            return blobName;
        }

        @Override
        public long size() {
            return fileSize;
        }

        private long chunkSize() {
            return FileTransferHelper.calculateChunkSize(fileSize);
        }

        @Override
        public InputStream openStream() {

            if (isOpened && (isDirectory || (currentOffset >= fileSize)))
                return null;

            InputStream stream;
            if (isDirectory || (fileSize <= chunkSize())) {
                stream = s3Connector.getObject(bucketName, blobName);
            } else {
                stream = s3Connector.getObject(bucketName, blobName, currentOffset, chunkSize());
            }

            if (!isDirectory) {
                int responseSize;
                try {
                    responseSize = stream.available();
                } catch (Exception e) {
                    throw new EdcException("Error reading response size", e);
                }

                currentOffset += responseSize;
            }

            if (!isOpened)
                isOpened = true;
            return stream;
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

        public Builder client(S3Connector s3Connector) {
            source.s3Connector = s3Connector;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            source.monitor = monitor;
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

        public Builder filterIncludes(String filterIncludes) {
            if (!StringUtils.isNullOrBlank(filterIncludes))
                source.filterIncludes = Pattern.compile(filterIncludes);
            return this;
        }

        public Builder filterExcludes(String filterExcludes) {
            if (!StringUtils.isNullOrBlank(filterExcludes))
                source.filterExcludes = Pattern.compile(filterExcludes);
            return this;
        }

        public IonosDataSource build() {
            return source;
        }
    }
}
