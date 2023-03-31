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

package org.eclipse.edc.test.system.remote;

import io.gatling.javaapi.core.Simulation;
import org.eclipse.edc.test.system.FileTransferSimulationConfiguration;

import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.edc.test.system.local.FileTransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.DESCRIPTION;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.contractNegotiationAndTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from environment variables.
 */
public class FileTransferAsClientSimulation extends Simulation {

    public FileTransferAsClientSimulation() {
        setUp(scenario(DESCRIPTION)
                .repeat(1)
                .on(
                        contractNegotiationAndTransfer(
                                getFromEnv("PROVIDER_URL"),
                                new FileTransferSimulationConfiguration(getFromEnv("DESTINATION_PATH")))
                )
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(getFromEnv("CONSUMER_URL") + "/" + CONSUMER_MANAGEMENT_PATH))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }

    private static String getFromEnv(String env) {
        return Objects.requireNonNull(System.getenv(env), env + " must be set.");
    }
}
