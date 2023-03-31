/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core;

import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.system.Hostname;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.core.CoreServicesExtension.HOSTNAME_SETTING;

@ExtendWith(EdcExtension.class)
class CoreServicesExtensionIntegrationTest {

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(HOSTNAME_SETTING, "hostname"));
    }

    @Test
    void shouldProvideHostnameExtension(Hostname hostname) {
        assertThat(hostname.get()).isEqualTo("hostname");
    }

}