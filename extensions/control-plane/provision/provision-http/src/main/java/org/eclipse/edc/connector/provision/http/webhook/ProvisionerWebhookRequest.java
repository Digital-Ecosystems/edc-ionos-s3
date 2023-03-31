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

package org.eclipse.edc.connector.provision.http.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.Polymorphic;

@JsonDeserialize(builder = ProvisionerWebhookRequest.Builder.class)
@JsonTypeName("dataspaceconnector:provisioner-callback-request")
public class ProvisionerWebhookRequest implements Polymorphic {
    @NotNull(message = "resourceDefinitionId cannot be null")
    private String resourceDefinitionId;
    private boolean hasToken;
    @NotNull(message = "assetId cannot be null")
    private String assetId;
    @NotNull(message = "resourceName cannot be null")
    private String resourceName;
    @NotNull(message = "contentDataAddress cannot be null")
    private DataAddress contentDataAddress;
    @NotNull(message = "apiKeyJwt cannot be null")
    private String apiKeyJwt;

    private ProvisionerWebhookRequest() {
    }

    public String getResourceDefinitionId() {
        return resourceDefinitionId;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public DataAddress getContentDataAddress() {
        return contentDataAddress;
    }

    @JsonProperty("hasToken")
    public boolean hasToken() {
        return hasToken;
    }

    public String getApiKeyJwt() {
        return apiKeyJwt;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final ProvisionerWebhookRequest request;

        private Builder() {
            request = new ProvisionerWebhookRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder resourceName(String resourceName) {
            request.resourceName = resourceName;
            return this;
        }

        public Builder contentDataAddress(DataAddress contentDataAddress) {
            request.contentDataAddress = contentDataAddress;
            return this;
        }

        public Builder resourceDefinitionId(String resourceDefinitionId) {
            request.resourceDefinitionId = resourceDefinitionId;
            return this;
        }

        public Builder hasToken(boolean hasToken) {
            request.hasToken = hasToken;
            return this;
        }

        public Builder apiKeyJwt(String token) {
            request.apiKeyJwt = token;
            return this;
        }

        public ProvisionerWebhookRequest build() {
            return request;
        }
    }
}
