/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * When a DID URL gets resolved from ION, this object represents the JSON that is returned.
 */
@JsonDeserialize(builder = DidDocument.Builder.class)
public class DidDocument {
    private final List<Service> service = new ArrayList<>();
    private String id;
    @JsonProperty("@context")
    private List<Object> context = Collections.singletonList("https://w3id.org/did-resolution/v1");
    private List<VerificationMethod> verificationMethod = new ArrayList<>();
    private List<String> authentication = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getContext() {
        return context;
    }

    public List<Service> getService() {
        return service;
    }

    public List<VerificationMethod> getVerificationMethod() {
        return verificationMethod;
    }

    public List<String> getAuthentication() {
        return authentication;
    }

    @Override
    public String toString() {
        return getId();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final DidDocument document;

        private Builder() {
            document = new DidDocument();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            document.id = id;
            return this;
        }

        @JsonProperty("@context")
        public Builder context(List<Object> context) {
            document.context = context;
            return this;
        }

        public Builder service(List<Service> services) {
            document.service.addAll(services);
            return this;
        }

        public Builder verificationMethod(List<VerificationMethod> verificationMethod) {
            document.verificationMethod = verificationMethod;
            return this;
        }

        public Builder verificationMethod(String id, String type, EllipticCurvePublicKey publicKey) {
            document.verificationMethod.add(VerificationMethod.Builder.create()
                    .id(id)
                    .type(type)
                    //.publicKeyJwk(new EllipticCurvePublicKey(publicKey.getCurve().getName(), publicKey.getKeyType().getValue(), publicKey.getX().toString(), publicKey.getY().toString()))
                    .publicKeyJwk(publicKey)
                    .build());
            return this;
        }

        public Builder authentication(List<String> authentication) {
            document.authentication = authentication;
            return this;
        }

        public DidDocument build() {
            return document;
        }
    }
}


