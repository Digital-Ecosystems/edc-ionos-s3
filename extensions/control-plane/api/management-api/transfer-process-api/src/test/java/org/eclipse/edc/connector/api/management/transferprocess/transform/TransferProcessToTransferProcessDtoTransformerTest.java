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

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import org.eclipse.edc.connector.api.management.transferprocess.model.DataAddressInformationDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.UNSAVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessToTransferProcessDtoTransformerTest {

    private final TransferProcessTransformerTestData data = new TransferProcessTransformerTestData();
    private final TransferProcessToTransferProcessDtoTransformer transformer = new TransferProcessToTransferProcessDtoTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcess.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(TransferProcessDto.class);
    }

    @Test
    void transform() {
        when(data.context.transform(any(), eq(DataRequestDto.class))).thenReturn(data.dataRequestDto);

        var result = transformer.transform(data.entity.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto.build());
        verify(data.context, never()).reportProblem(any());
    }

    @Test
    void transform_whenInvalidState() {
        when(data.context.transform(any(), eq(DataRequestDto.class))).thenReturn(data.dataRequestDto);

        data.entity.state(invalidStateCode());
        data.dto.state(null);

        when(data.context.transform(any(), eq(DataRequestDto.class))).thenReturn(data.dataRequestDto);

        var result = transformer.transform(data.entity.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto.build());
        verify(data.context).reportProblem("Invalid value for TransferProcess.state");
    }

    @Test
    void transform_whenMinimalData() {
        when(data.context.transform(any(), eq(DataRequestDto.class))).thenReturn(data.dataRequestDto);
        data.dto.state(UNSAVED.name());

        data.dataDestination = DataAddress.Builder.newInstance().type(data.dataDestinationType);
        data.dataRequest = DataRequest.Builder.newInstance()
                .dataDestination(data.dataDestination.build())
                .build();
        data.entity = TransferProcess.Builder.newInstance()
                .id(data.id)
                .type(data.type)
                .stateTimestamp(data.stateTimestamp)
                .createdAt(data.createdTimestamp)
                .dataRequest(data.dataRequest);
        data.dto
                .dataDestination(
                        DataAddressInformationDto.Builder.newInstance()
                                .properties(Map.of("type", data.dataDestinationType))
                                .build())
                .stateTimestamp(data.stateTimestamp)
                .errorDetail(null);

        when(data.context.transform(any(), eq(DataRequestDto.class))).thenReturn(data.dataRequestDto);

        var result = transformer.transform(data.entity.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto.build());
    }

    private int invalidStateCode() {
        var stateCode = 0;
        while (TransferProcessStates.from(stateCode) != null) {
            stateCode++;
        }
        return stateCode;
    }
}
