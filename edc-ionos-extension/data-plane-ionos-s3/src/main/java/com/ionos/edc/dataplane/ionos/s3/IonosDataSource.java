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
import com.ionos.edc.extension.s3.api.S3Object;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.util.string.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

class IonosDataSource implements DataSource {

    private S3ConnectorApi s3Api;
    private String bucketName;
    private String blobName;
    private Pattern filterIncludes;
    private Pattern filterExcludes;
    
    private IonosDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {

        var objects = s3Api.listObjects(this.bucketName, this.blobName);

        if (objects.isEmpty()) {
            return failure(new StreamFailure(
                    List.of("No files found in bucket " + bucketName + " with blobName " + blobName), GENERAL_ERROR)
            );
        }

        if (this.filterIncludes != null) {
            objects = objects.stream()
                    .filter(object -> applyFilterIncludes(object))
                    .collect(Collectors.toList());
        }

        if (this.filterExcludes != null) {
            objects = objects.stream()
                    .filter(object -> ! applyFilterExcludes(object))
                    .collect(Collectors.toList());
        }

        List<Part> partStream = objects.stream()
                .map(object -> new S3Part(this.s3Api, this.bucketName, object.objectName(), object.size()))
                .collect(Collectors.toList());

        return success(partStream.stream());
    }

    boolean applyFilterIncludes(S3Object object) {
        if (object.isRootObject(blobName))
            return true;

        return filterIncludes.matcher(object.shortObjectName(blobName)).find();
    }
    boolean applyFilterExcludes(S3Object object) {
        if (object.isRootObject(blobName))
            return true;

        return !filterExcludes.matcher(object.shortObjectName(blobName)).find();
    }

    private static class S3Part implements Part {

        private final S3ConnectorApi s3Api;
        private final String bucketName;
        private final String blobName;
        private final long size;

        S3Part(S3ConnectorApi s3Api, String bucketName, String blobName, long size) {
            super();
            this.s3Api = s3Api;
            this.bucketName = bucketName;
            this.blobName = blobName;
            this.size = size;
        }

        @Override
        public String name() {
            return blobName;
        }

        @Override
        public InputStream openStream() {
            return s3Api.getObject(bucketName, blobName);
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
