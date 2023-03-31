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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.transfer.spi.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.CANCELLED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.ENDED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.ERROR;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STREAMING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.UNSAVED;

/**
 * Represents a data transfer process.
 * <p>
 * A data transfer process exists on both the consumer and provider connector; it is a representation of the data
 * sharing transaction from the perspective of each endpoint. The data transfer process is modeled as a "loosely"
 * coordinated state machine on each connector. The state transitions are symmetric on the consumer and provider with
 * the exception that the consumer process has two additional states for request/request ack.
 * <p>
 * The consumer transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#REQUESTING} -&gt;
 * {@link TransferProcessStates#REQUESTED} -&gt;
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#ENDED} -&gt;
 * {@link TransferProcessStates#CANCELLED} -&gt; optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <p>
 * <p>
 * The provider transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#ENDED} -&gt;
 * {@link TransferProcessStates#CANCELLED} -&gt; optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <p>
 */
@JsonTypeName("dataspaceconnector:transferprocess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess extends StatefulEntity<TransferProcess> {

    private Type type = Type.CONSUMER;
    private DataRequest dataRequest;
    private DataAddress contentDataAddress;
    private ResourceManifest resourceManifest;
    private ProvisionedResourceSet provisionedResourceSet;
    private List<DeprovisionedResource> deprovisionedResources = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();


    private TransferProcess() {
    }

    public List<DeprovisionedResource> getDeprovisionedResources() {
        return deprovisionedResources;
    }

    public Type getType() {
        return type;
    }

    public DataRequest getDataRequest() {
        return dataRequest;
    }

    public ResourceManifest getResourceManifest() {
        return resourceManifest;
    }

    public ProvisionedResourceSet getProvisionedResourceSet() {
        return provisionedResourceSet;
    }

    public DataAddress getContentDataAddress() {
        return contentDataAddress;
    }

    public void setContentDataAddress(DataAddress dataAddress) {
        contentDataAddress = dataAddress;
    }

    public void transitionInitial() {
        transition(INITIAL, UNSAVED);
    }

    public void transitionProvisioning(ResourceManifest manifest) {
        transition(PROVISIONING, INITIAL, PROVISIONING);
        resourceManifest = manifest;
        resourceManifest.setTransferProcessId(id);
    }

    public void addProvisionedResource(ProvisionedResource resource) {
        if (provisionedResourceSet == null) {
            provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance().transferProcessId(id).build();
        }
        provisionedResourceSet.addResource(resource);
        setModified();

    }

    public void addDeprovisionedResource(DeprovisionedResource resource) {
        deprovisionedResources.add(resource);
        setModified();
    }

    @Nullable
    public ProvisionedResource getProvisionedResource(String id) {
        if (provisionedResourceSet == null) {
            return null;
        }
        return provisionedResourceSet.getResources().stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns the collection of resources that have not been provisioned.
     */
    @JsonIgnore
    @NotNull
    public List<ResourceDefinition> getResourcesToProvision() {
        if (resourceManifest == null) {
            return emptyList();
        }
        if (provisionedResourceSet == null) {
            return unmodifiableList(resourceManifest.getDefinitions());
        }
        var provisionedResources = provisionedResourceSet.getResources().stream().map(ProvisionedResource::getResourceDefinitionId).collect(toSet());
        return resourceManifest.getDefinitions().stream().filter(r -> !provisionedResources.contains(r.getId())).collect(toList());
    }

    public boolean provisioningComplete() {
        if (resourceManifest == null) {
            return false;
        }

        return getResourcesToProvision().isEmpty();
    }

    /**
     * Returns the collection of resources that have not been deprovisioned.
     */
    @JsonIgnore
    @NotNull
    public List<ProvisionedResource> getResourcesToDeprovision() {
        if (provisionedResourceSet == null) {
            return emptyList();
        }

        var deprovisionedResources = this.deprovisionedResources.stream().map(DeprovisionedResource::getProvisionedResourceId).collect(toSet());
        return provisionedResourceSet.getResources().stream().filter(r -> !deprovisionedResources.contains(r.getId())).collect(toList());
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public boolean deprovisionComplete() {
        return getResourcesToDeprovision().isEmpty();
    }

    public void transitionProvisioningRequested() {
        transition(PROVISIONING_REQUESTED, PROVISIONING);
    }

    public void transitionProvisioned() {
        // requested is allowed to support retries
        transition(PROVISIONED, PROVISIONING, PROVISIONING_REQUESTED, PROVISIONED, REQUESTED);
    }

    public void transitionRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTING state");
        }
        transition(REQUESTING, PROVISIONED, REQUESTING);
    }

    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTED state");
        }
        transition(REQUESTED, PROVISIONED, REQUESTING, REQUESTED);

    }

    public void transitionInProgressOrStreaming() {
        var dataRequest = getDataRequest();
        if (dataRequest.getTransferType().isFinite()) {
            transitionInProgress();
        } else {
            transitionStreaming();
        }
    }

    public void transitionInProgress() {
        if (type == Type.CONSUMER) {
            // the consumer must first transition to the request/ack states before in progress
            transition(IN_PROGRESS, REQUESTED, IN_PROGRESS);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(IN_PROGRESS, REQUESTED, PROVISIONED, IN_PROGRESS);
        }
    }

    public void transitionStreaming() {
        if (type == Type.CONSUMER) {
            // the consumer must first transition to the request/ack states before in progress
            transition(STREAMING, REQUESTED, STREAMING);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(STREAMING, PROVISIONED, STREAMING);
        }
    }

    public void transitionCompleted() {
        // consumers are in REQUESTED state after sending a request to the provider, they can directly transition to COMPLETED when the transfer is complete
        transition(COMPLETED, COMPLETED, IN_PROGRESS, REQUESTED, STREAMING);
    }

    public void transitionDeprovisioning() {
        transition(DEPROVISIONING, COMPLETED, DEPROVISIONING);
    }

    public void transitionDeprovisioningRequested() {
        transition(DEPROVISIONING_REQUESTED, DEPROVISIONING);
    }

    public void transitionDeprovisioned() {
        transition(DEPROVISIONED, DEPROVISIONING, DEPROVISIONING_REQUESTED, DEPROVISIONED);
    }

    public void transitionCancelled() {
        var forbiddenStates = List.of(ENDED, COMPLETED, ERROR);

        var allowedStates = Arrays.stream(TransferProcessStates.values())
                .filter(it -> !forbiddenStates.contains(it))
                .toArray(TransferProcessStates[]::new);

        transition(CANCELLED, allowedStates);
    }

    public void transitionEnded() {
        transition(ENDED, DEPROVISIONED);
    }

    public void transitionError(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        state = ERROR.code();
        stateCount = 1;
        updateStateTimestamp();
        setModified();
    }

    @Override
    public TransferProcess copy() {
        var builder = Builder.newInstance()
                .resourceManifest(resourceManifest)
                .dataRequest(dataRequest)
                .provisionedResourceSet(provisionedResourceSet)
                .contentDataAddress(contentDataAddress)
                .deprovisionedResources(deprovisionedResources)
                .properties(properties)
                .type(type);
        return copy(builder);
    }

    public Builder toBuilder() {
        return new Builder(copy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferProcess that = (TransferProcess) o;
        return id.equals(that.id);
    }

    @Override
    public String toString() {
        return "TransferProcess{" +
                "id='" + id + '\'' +
                ", state=" + TransferProcessStates.from(state) +
                ", stateTimestamp=" + Instant.ofEpochMilli(stateTimestamp) +
                '}';
    }

    private void transition(TransferProcessStates end, TransferProcessStates... starts) {
        if (end.code() < state) {
            return; //we cannot transition "back"
        }

        if (Arrays.stream(starts).noneMatch(s -> s.code() == state)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", TransferProcessStates.from(state), TransferProcessStates.from(end.code())));
        }
        transitionTo(end.code());
    }

    public enum Type {
        CONSUMER, PROVIDER
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<TransferProcess, Builder> {

        private Builder(TransferProcess process) {
            super(process);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new TransferProcess());
        }

        public Builder type(Type type) {
            entity.type = type;
            return this;
        }

        public Builder dataRequest(DataRequest request) {
            entity.dataRequest = request;
            return this;
        }

        public Builder resourceManifest(ResourceManifest manifest) {
            entity.resourceManifest = manifest;
            return this;
        }

        public Builder contentDataAddress(DataAddress dataAddress) {
            entity.contentDataAddress = dataAddress;
            return this;
        }

        public Builder provisionedResourceSet(ProvisionedResourceSet set) {
            entity.provisionedResourceSet = set;
            return this;
        }

        public Builder deprovisionedResources(List<DeprovisionedResource> resources) {
            entity.deprovisionedResources = resources;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            entity.properties = properties;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public TransferProcess build() {
            if (entity.resourceManifest != null) {
                entity.resourceManifest.setTransferProcessId(entity.id);
            }

            if (entity.provisionedResourceSet != null) {
                entity.provisionedResourceSet.setTransferProcessId(entity.id);
            }

            if (entity.dataRequest != null) {
                entity.dataRequest.associateWithProcessId(entity.id);
            }
            return super.build();
        }

    }
}
