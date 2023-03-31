/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.management.transferprocess.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(builder = TransferRequestDto.Builder.class)
public class TransferRequestDto {

    private String id;
    @NotNull(message = "connectorAddress cannot be null")
    private String connectorAddress;
    @NotNull(message = "contractId cannot be null")
    private String contractId;
    @NotNull(message = "dataDestination cannot be null")
    private DataAddress dataDestination;
    private boolean managedResources = true;
    private Map<String, String> properties = new HashMap<>();
    @NotNull(message = "transferType cannot be null")
    private TransferType transferType = new TransferType();
    @NotNull(message = "protocol cannot be null")
    private String protocol = "ids-multipart";
    @NotNull(message = "connectorId cannot be null")
    private String connectorId;
    @NotNull(message = "assetId cannot be null")
    private String assetId;

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public DataAddress getDataDestination() {
        return dataDestination;
    }

    public boolean isManagedResources() {
        return managedResources;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getAssetId() {
        return assetId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferRequestDto request;

        private Builder() {
            request = new TransferRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            request.connectorAddress = connectorAddress;
            return this;
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder contractId(String contractId) {
            request.contractId = contractId;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            request.dataDestination = dataDestination;
            return this;
        }

        public Builder managedResources(boolean managedResources) {
            request.managedResources = managedResources;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            request.properties = properties;
            return this;
        }

        public Builder transferType(TransferType transferType) {
            request.transferType = transferType;
            return this;
        }

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            request.connectorId = connectorId;
            return this;
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public TransferRequestDto build() {
            return request;
        }
    }
}
