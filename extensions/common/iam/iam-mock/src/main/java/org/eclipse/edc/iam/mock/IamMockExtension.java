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

package org.eclipse.edc.iam.mock;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * An IAM provider mock used for testing.
 */
@Provides(IdentityService.class)
@Extension(value = IamMockExtension.NAME)
public class IamMockExtension implements ServiceExtension {

    public static final String NAME = "Mock IAM";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var region = context.getSetting("edc.mock.region", "eu");
        context.registerService(IdentityService.class, new MockIdentityService(context.getTypeManager(), region));
    }
}
