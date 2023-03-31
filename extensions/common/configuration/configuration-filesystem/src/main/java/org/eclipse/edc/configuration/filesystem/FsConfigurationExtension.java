/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.configuration.filesystem;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.lang.String.format;
import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;

/**
 * Sources configuration values from a properties file.
 */
@Extension(value = FsConfigurationExtension.NAME)
public class FsConfigurationExtension implements ConfigurationExtension {

    public static final String NAME = "FS Configuration";
    @Setting
    private static final String FS_CONFIG = "edc.fs.config";
    private Config config;
    private Path configFile;

    /**
     * Default ctor - required for extension loading
     */
    public FsConfigurationExtension() {
    }

    /**
     * Testing ctor
     */
    FsConfigurationExtension(Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(Monitor monitor) {
        var configLocation = propOrEnv(FS_CONFIG, "dataspaceconnector-configuration.properties");
        var configPath = configFile != null ? configFile : Paths.get(configLocation);

        if (!Files.exists(configPath)) {
            monitor.warning(format("Configuration file does not exist: %s. Ignoring.", configLocation));
            return;
        }

        try (InputStream is = Files.newInputStream(configPath)) {
            var properties = new Properties();
            properties.load(is);
            config = ConfigFactory.fromProperties(properties);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Config getConfig() {
        return config;
    }
}
