/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - Added initiate-negotiation endpoint
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Improvements
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationState;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })

@Path("/contractnegotiations")
public class ContractNegotiationApiController implements ContractNegotiationApi {
    private final Monitor monitor;
    private final ContractNegotiationService service;
    private final DtoTransformerRegistry transformerRegistry;

    public ContractNegotiationApiController(Monitor monitor, ContractNegotiationService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    @Deprecated
    public List<ContractNegotiationDto> getNegotiations(@Valid @BeanParam QuerySpecDto querySpecDto) {
        return queryContractNegotiations(querySpecDto);
    }

    @POST
    @Path("/request")
    @Override
    public List<ContractNegotiationDto> queryNegotiations(@Valid QuerySpecDto querySpecDto) {
        return queryContractNegotiations(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Path("/{id}")
    @Override
    public ContractNegotiationDto getNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation with id %s", id));

        return Optional.of(id)
                .map(service::findbyId)
                .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public NegotiationState getNegotiationState(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation state with id %s", id));
        return Optional.of(id)
                .map(service::getState)
                .map(NegotiationState::new)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public ContractAgreementDto getAgreementForNegotiation(@PathParam("id") String negotiationId) {
        monitor.debug(format("Get contract agreement of negotiation with id %s", negotiationId));

        return Optional.of(negotiationId)
                .map(service::getForNegotiation)
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, negotiationId));
    }

    @POST
    @Override
    public IdResponseDto initiateContractNegotiation(@Valid NegotiationInitiateRequestDto initiateDto) {
        var transformResult = transformerRegistry.transform(initiateDto, ContractOfferRequest.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }

        var request = transformResult.getContent();

        var contractNegotiation = service.initiateNegotiation(request);
        return IdResponseDto.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();
    }

    @POST
    @Path("/{id}/cancel")
    @Override
    public void cancelNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Attempting to cancel contract definition with id %s", id));
        var result = service.cancel(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
        monitor.debug(format("Contract negotiation canceled %s", result.getId()));
    }

    @POST
    @Path("/{id}/decline")
    @Override
    public void declineNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Attempting to decline contract definition with id %s", id));
        var result = service.decline(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
        monitor.debug(format("Contract negotiation declined %s", result.getId()));
    }

    private List<ContractNegotiationDto> queryContractNegotiations(QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("Get all contract definitions %s", spec));

        try (var stream = service.query(spec).orElseThrow(exceptionMapper(ContractDefinition.class, null))) {
            return stream
                    .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(Collectors.toList());
        }
    }

}
