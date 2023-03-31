/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *       Siemens AG - added additionalHeaders
 *
 */

package org.eclipse.edc.spi.types.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

/**
 * This is a wrapper class for the {@link DataAddress} object, which has typed accessors for properties specific to
 * a http endpoint.
 */
@JsonTypeName()
@JsonDeserialize(builder = DataAddress.Builder.class)
public class HttpDataAddress extends DataAddress {

    public static final String HTTP_DATA = "HttpData";

    private static final String NAME = "name";
    private static final String PATH = "path";
    private static final String QUERY_PARAMS = "queryParams";
    private static final String METHOD = "method";
    private static final String BASE_URL = "baseUrl";
    private static final String AUTH_KEY = "authKey";
    private static final String AUTH_CODE = "authCode";
    private static final String SECRET_NAME = "secretName";
    private static final String PROXY_BODY = "proxyBody";
    private static final String PROXY_PATH = "proxyPath";
    private static final String PROXY_QUERY_PARAMS = "proxyQueryParams";
    private static final String PROXY_METHOD = "proxyMethod";
    public static final String ADDITIONAL_HEADER = "header:";
    public static final String CONTENT_TYPE = "contentType";
    public static final String OCTET_STREAM = "application/octet-stream";
    public static final String NON_CHUNKED_TRANSFER = "nonChunkedTransfer";
    public static final Set<String> ADDITIONAL_HEADERS_TO_IGNORE = Set.of("content-type");

    private HttpDataAddress() {
        super();
        this.setType(HTTP_DATA);
    }

    @JsonIgnore
    public String getName() {
        return getProperty(NAME);
    }

    @JsonIgnore
    public String getBaseUrl() {
        return getProperty(BASE_URL);
    }

    @JsonIgnore
    public String getPath() {
        return getProperty(PATH);
    }

    @JsonIgnore
    public String getQueryParams() {
        return getProperty(QUERY_PARAMS);
    }

    @JsonIgnore
    public String getMethod() {
        return getProperty(METHOD);
    }

    @JsonIgnore
    public String getAuthKey() {
        return getProperty(AUTH_KEY);
    }

    @JsonIgnore
    public String getAuthCode() {
        return getProperty(AUTH_CODE);
    }

    @JsonIgnore
    public String getSecretName() {
        return getProperty(SECRET_NAME);
    }

    @JsonIgnore
    public String getProxyBody() {
        return getProperty(PROXY_BODY);
    }

    @JsonIgnore
    public String getProxyPath() {
        return getProperty(PROXY_PATH);
    }

    @JsonIgnore
    public String getProxyQueryParams() {
        return getProperty(PROXY_QUERY_PARAMS);
    }

    @JsonIgnore
    public String getProxyMethod() {
        return getProperty(PROXY_METHOD);
    }

    @JsonIgnore
    public String getContentType() {
        return getProperty(CONTENT_TYPE, OCTET_STREAM);
    }

    @JsonIgnore
    public Map<String, String> getAdditionalHeaders() {
        return getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ADDITIONAL_HEADER))
                .collect(Collectors.toMap(entry -> entry.getKey().replace(ADDITIONAL_HEADER, ""), Map.Entry::getValue));

    }

    @JsonIgnore
    public boolean getNonChunkedTransfer() {
        return Optional.of(NON_CHUNKED_TRANSFER)
                .map(this::getProperty)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends DataAddress.Builder<HttpDataAddress, Builder> {

        private Builder() {
            super(new HttpDataAddress());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            this.property(NAME, name);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.property(BASE_URL, baseUrl);
            return this;
        }

        public Builder path(String path) {
            this.property(PATH, path);
            return this;
        }

        public Builder queryParams(String queryParams) {
            this.property(QUERY_PARAMS, queryParams);
            return this;
        }

        public Builder method(String method) {
            this.property(METHOD, method);
            return this;
        }

        public Builder authKey(String authKey) {
            this.property(AUTH_KEY, authKey);
            return this;
        }

        public Builder authCode(String authCode) {
            this.property(AUTH_CODE, authCode);
            return this;
        }

        public Builder secretName(String secretName) {
            this.property(SECRET_NAME, secretName);
            return this;
        }

        public Builder proxyBody(String proxyBody) {
            this.property(PROXY_BODY, proxyBody);
            return this;
        }

        public Builder proxyPath(String proxyPath) {
            this.property(PROXY_PATH, proxyPath);
            return this;
        }

        public Builder proxyQueryParams(String proxyQueryParams) {
            this.property(PROXY_QUERY_PARAMS, proxyQueryParams);
            return this;
        }

        public Builder proxyMethod(String proxyMethod) {
            this.property(PROXY_METHOD, proxyMethod);
            return this;
        }

        public Builder addAdditionalHeader(String additionalHeaderName, String additionalHeaderValue) {
            if (ADDITIONAL_HEADERS_TO_IGNORE.contains(additionalHeaderName.toLowerCase())) {
                return this;
            }

            address.getProperties().put(ADDITIONAL_HEADER + additionalHeaderName, Objects.requireNonNull(additionalHeaderValue));
            return this;
        }

        public Builder contentType(String contentType) {
            this.property(CONTENT_TYPE, contentType);
            return this;
        }

        public Builder nonChunkedTransfer(boolean nonChunkedTransfer) {
            this.property(NON_CHUNKED_TRANSFER, String.valueOf(nonChunkedTransfer));
            return this;
        }

        public Builder copyFrom(DataAddress other) {
            Optional.ofNullable(other).map(DataAddress::getProperties).orElse(emptyMap()).forEach(this::property);
            return this;
        }

        @Override
        public HttpDataAddress build() {
            this.type(HTTP_DATA);
            return address;
        }
    }
}
