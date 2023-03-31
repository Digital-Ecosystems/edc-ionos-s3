/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import com.azure.core.credential.AzureSasCredential;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Writes data into an Azure storage container.
 */
public class AzureStorageDataSink extends ParallelSink {
    // Name of the empty blob used to indicate completion. Used by consumer-side status checker.
    public static final String COMPLETE_BLOB_NAME = ".complete";

    private String accountName;
    private String containerName;
    private String sharedAccessSignature;
    private BlobStoreApi blobStoreApi;

    /**
     * Writes data into an Azure storage container.
     */
    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            String blobName = part.name();
            try (var input = part.openStream()) {
                try (var output = blobStoreApi.getBlobAdapter(accountName, containerName, blobName, new AzureSasCredential(sharedAccessSignature))
                        .getOutputStream()) {
                    try {
                        input.transferTo(output);
                    } catch (Exception e) {
                        return getTransferResult(e, "Error transferring blob for %s on account %s", blobName, accountName);
                    }
                } catch (Exception e) {
                    return getTransferResult(e, "Error creating blob for %s on account %s", blobName, accountName);
                }
            } catch (Exception e) {
                return getTransferResult(e, "Error reading blob %s", blobName);
            }
        }
        return StatusResult.success();
    }

    @Override
    protected StatusResult<Void> complete() {
        try {
            // Write an empty blob to indicate completion
            blobStoreApi.getBlobAdapter(accountName, containerName, COMPLETE_BLOB_NAME, new AzureSasCredential(sharedAccessSignature))
                    .getOutputStream().close();
        } catch (Exception e) {
            return getTransferResult(e, "Error creating blob %s on account %s", COMPLETE_BLOB_NAME, accountName);
        }
        return super.complete();
    }

    @NotNull
    private StatusResult<Void> getTransferResult(Exception e, String logMessage, Object... args) {
        String message = format(logMessage, args);
        monitor.severe(message, e);
        return StatusResult.failure(ERROR_RETRY, message);
    }

    private AzureStorageDataSink() {
    }

    public static class Builder extends ParallelSink.Builder<Builder, AzureStorageDataSink> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            sink.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            sink.containerName = containerName;
            return this;
        }

        public Builder sharedAccessSignature(String sharedAccessSignature) {
            sink.sharedAccessSignature = sharedAccessSignature;
            return this;
        }

        public Builder blobStoreApi(BlobStoreApi blobStoreApi) {
            sink.blobStoreApi = blobStoreApi;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.accountName, "accountName");
            Objects.requireNonNull(sink.containerName, "containerName");
            Objects.requireNonNull(sink.sharedAccessSignature, "sharedAccessSignature");
            Objects.requireNonNull(sink.blobStoreApi, "blobStoreApi");
        }

        private Builder() {
            super(new AzureStorageDataSink());
        }
    }
}
