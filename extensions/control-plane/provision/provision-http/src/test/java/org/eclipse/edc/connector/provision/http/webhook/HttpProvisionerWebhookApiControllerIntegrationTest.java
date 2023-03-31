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

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

@ApiTest
@ExtendWith(EdcExtension.class)
class HttpProvisionerWebhookApiControllerIntegrationTest {

    private static final String PROVISIONER_BASE_PATH = "/api/v1/provisioner";
    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.provisioner.port", String.valueOf(port),
                "web.http.provisioner.path", PROVISIONER_BASE_PATH,
                "edc.api.auth.key", authKey
        ));
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidRequestParams.class)
    void callProvisionWebhook_invalidBody(String assetId, DataAddress cda, String resName, String resDefId, String token) {
        var tpId = "tpId";
        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId(assetId)
                .contentDataAddress(cda)
                .resourceName(resName)
                .apiKeyJwt(token)
                .resourceDefinitionId(resDefId)
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/provision", Map.of("processId", tpId))
                .then()
                .statusCode(400)
                .body(containsString(""));
    }

    @Test
    void callProvisionWebhook(TransferProcessStore store) {

        store.save(createTransferProcess());

        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId("test-asset")
                .contentDataAddress(dataAddress())
                .apiKeyJwt("test-token")
                .resourceName("resource-name")
                .resourceDefinitionId("resource-definition")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/provision", "tp-id")
                .then()
                .statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)))
                .body(anything());
    }

    @Test
    void callDeprovisionWebhook_invalidBody() {

        baseRequest()
                .contentType("application/json")
                .post("/callback/{processId}/deprovision", "tp-id")
                .then()
                .statusCode(equalTo(400))
                .body(anything());
    }

    @Test
    void callDeprovisionWebhook_notFound() {

        var rq = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("resource-id")
                .errorMessage("some-error")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/deprovision", "tp-id")
                .then()
                .statusCode(equalTo(404))
                .body(anything());
    }

    @Test
    void callDeprovisionWebhook(TransferProcessStore store) {

        store.save(createTransferProcess());

        var rq = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("resource-id")
                .errorMessage("some-error")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/deprovision", "tp-id")
                .then()
                .statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)))
                .body(anything());
    }

    private DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type("foo").build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath(PROVISIONER_BASE_PATH)
                .header("x-api-key", authKey)
                .when();
    }

    private TransferProcess createTransferProcess() {
        return createTransferProcessBuilder().build();
    }

    private TransferProcess.Builder createTransferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .id("tp-id")
                .state(TransferProcessStates.IN_PROGRESS.code())
                .type(TransferProcess.Type.PROVIDER)
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType("file")
                        .build());
    }

    private static class InvalidRequestParams implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(null, dataAddress(), "resourcename", "resourcedef", "token"),
                    Arguments.of("assetid", null, "resourcename", "resourcedef", "token"),
                    Arguments.of("assetid", dataAddress(), null, "resourcedef", "token"),
                    Arguments.of("assetid", dataAddress(), "resourcename", null, "token"),
                    Arguments.of("assetid", dataAddress(), "resourcename", "resourcedef", null)
            );
        }

        private DataAddress dataAddress() {
            return DataAddress.Builder.newInstance().type("foo").build();
        }
    }

}
