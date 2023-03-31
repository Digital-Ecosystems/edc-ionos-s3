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

package org.eclipse.edc.test.system.local;

import org.eclipse.edc.test.system.FileTransferSimulationConfiguration;

import java.io.File;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class FileTransferLocalSimulation extends TransferLocalSimulation {
    public static final String CONSUMER_ASSET_PATH = new File(tempDirectory(), "output.txt").getAbsolutePath();
    public static final String PROVIDER_ASSET_PATH = format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_FILE);

    public FileTransferLocalSimulation() {
        super(new FileTransferSimulationConfiguration(CONSUMER_ASSET_PATH));
    }
}
