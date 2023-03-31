/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.types.offer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A contract offer is exchanged between two participant agents. It describes the which assets the consumer may use, and
 * the rules and policies that apply to each asset.
 */
@JsonDeserialize(builder = ContractOffer.Builder.class)
public class ContractOffer {
    private String id;

    /**
     * The policy that describes the usage conditions of the assets
     */
    private Policy policy;

    /**
     * The offered asset
     */
    private Asset asset;
    /**
     * The participant who provides the offered data
     */
    private URI provider;
    /**
     * The participant consuming the offered data
     */
    private URI consumer;
    /**
     * Timestamp defining the start time when the offer becomes effective
     */
    private ZonedDateTime offerStart;
    /**
     * Timestamp defining the end date when the offer becomes ineffective
     */
    private ZonedDateTime offerEnd;
    /**
     * Timestamp defining the start date when the contract becomes effective
     */
    private ZonedDateTime contractStart;
    /**
     * Timestamp defining the end date when the contract becomes terminated
     */
    private ZonedDateTime contractEnd;


    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public URI getProvider() {
        return provider;
    }

    @Nullable
    public URI getConsumer() {
        return consumer;
    }

    @Nullable
    public ZonedDateTime getOfferStart() {
        return offerStart;
    }

    @Nullable
    public ZonedDateTime getOfferEnd() {
        return offerEnd;
    }

    @NotNull
    public ZonedDateTime getContractStart() {
        return contractStart;
    }

    @NotNull
    public ZonedDateTime getContractEnd() {
        return contractEnd;
    }

    @NotNull
    public Asset getAsset() {
        return asset;
    }

    @Nullable
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, policy, asset, provider, consumer, offerStart, offerEnd, contractStart, contractEnd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContractOffer that = (ContractOffer) o;
        return Objects.equals(id, that.id) && Objects.equals(policy, that.policy) && Objects.equals(asset, that.asset) && Objects.equals(provider, that.provider) &&
                Objects.equals(consumer, that.consumer) && Objects.equals(offerStart, that.offerStart) && Objects.equals(offerEnd, that.offerEnd) &&
                Objects.equals(contractStart, that.contractStart) && Objects.equals(contractEnd, that.contractEnd);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final ContractOffer contractOffer;

        private Builder() {
            contractOffer = new ContractOffer();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            contractOffer.id = id;
            return this;
        }

        public Builder provider(URI provider) {
            contractOffer.provider = provider;
            return this;
        }

        public Builder consumer(URI consumer) {
            contractOffer.consumer = consumer;
            return this;
        }

        public Builder asset(Asset asset) {
            contractOffer.asset = asset;
            return this;
        }

        public Builder offerStart(ZonedDateTime date) {
            contractOffer.offerStart = date;
            return this;
        }

        public Builder offerEnd(ZonedDateTime date) {
            contractOffer.offerEnd = date;
            return this;
        }

        public Builder contractStart(ZonedDateTime date) {
            contractOffer.contractStart = date;
            return this;
        }

        public Builder contractEnd(ZonedDateTime date) {
            contractOffer.contractEnd = date;
            return this;
        }

        public Builder policy(Policy policy) {
            contractOffer.policy = policy;
            return this;
        }

        public ContractOffer build() {
            Objects.requireNonNull(contractOffer.id);
            Objects.requireNonNull(contractOffer.asset, "Asset must not be null");
            Objects.requireNonNull(contractOffer.policy, "Policy must not be null");
            Objects.requireNonNull(contractOffer.contractStart, "Contract start must not be null");
            Objects.requireNonNull(contractOffer.contractEnd, "Contract end must not be null");
            return contractOffer;
        }
    }
}
