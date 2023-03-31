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
 *       Microsoft Corporation - Added initiate-transfer endpoint
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.asset;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetApiController implements AssetApi {

    private final Monitor monitor;
    private final AssetService service;
    private final DtoTransformerRegistry transformerRegistry;

    private final DataAddressResolver dataAddressResolver;

    public AssetApiController(Monitor monitor, AssetService service, DataAddressResolver dataAddressResolver, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.dataAddressResolver = dataAddressResolver;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Override
    public IdResponseDto createAsset(@Valid AssetEntryDto assetEntryDto) {
        var assetResult = transformerRegistry.transform(assetEntryDto.getAsset(), Asset.class);
        var dataAddressResult = transformerRegistry.transform(assetEntryDto.getDataAddress(), DataAddress.class);

        if (assetResult.failed() || dataAddressResult.failed()) {
            var errorMessages = Stream.concat(assetResult.getFailureMessages().stream(), dataAddressResult.getFailureMessages().stream());
            throw new InvalidRequestException(errorMessages.collect(toList()));
        }

        var dataAddress = dataAddressResult.getContent();
        var asset = assetResult.getContent();

        var resultContent = service.create(asset, dataAddress).orElseThrow(exceptionMapper(Asset.class, asset.getId()));

        monitor.debug(format("Asset created %s", assetEntryDto.getAsset()));
        return IdResponseDto.Builder.newInstance()
                .id(resultContent.getId())
                .createdAt(resultContent.getCreatedAt())
                .build();

    }

    @GET
    @Override
    @Deprecated
    public List<AssetResponseDto> getAllAssets(@Valid @BeanParam QuerySpecDto querySpecDto) {
        return queryAssets(querySpecDto);
    }

    @POST
    @Override
    @Path("/request")
    public List<AssetResponseDto> requestAssets(@Valid QuerySpecDto querySpecDto) {
        return queryAssets(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Path("{id}")
    @Override
    public AssetResponseDto getAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return Asset with id %s", id));
        return Optional.of(id)
                .map(it -> service.findById(id))
                .map(it -> transformerRegistry.transform(it, AssetResponseDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete Asset with id %s", id));
        service.delete(id).orElseThrow(exceptionMapper(Asset.class, id));
        monitor.debug(format("Asset deleted %s", id));
    }


    @GET
    @Path("{id}/address")
    @Override
    public DataAddressDto getAssetDataAddress(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return DataAddress of an Asset with id %s", id));
        return Optional.of(id)
                .map(it -> dataAddressResolver.resolveForAsset(id))
                .map(it -> transformerRegistry.transform(it, DataAddressDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
    }

    private List<AssetResponseDto> queryAssets(QuerySpecDto querySpecDto) {
        var transformationResult = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (transformationResult.failed()) {
            throw new InvalidRequestException(transformationResult.getFailureMessages());
        }

        var spec = transformationResult.getContent();

        monitor.debug(format("get all Assets from %s", spec));

        try (var assets = service.query(spec).orElseThrow(exceptionMapper(QuerySpec.class, null))) {
            return assets
                    .map(it -> transformerRegistry.transform(it, AssetResponseDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
        }
    }

}
