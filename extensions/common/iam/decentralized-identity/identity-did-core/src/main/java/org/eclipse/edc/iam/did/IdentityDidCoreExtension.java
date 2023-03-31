/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
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

package org.eclipse.edc.iam.did;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.edc.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.edc.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


@Provides({ DidResolverRegistry.class, DidPublicKeyResolver.class })
@Extension(value = IdentityDidCoreExtension.NAME)
public class IdentityDidCoreExtension implements ServiceExtension {

    public static final String NAME = "Identity Did Core";
    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var didResolverRegistry = new DidResolverRegistryImpl();
        context.registerService(DidResolverRegistry.class, didResolverRegistry);

        var publicKeyResolver = new DidPublicKeyResolverImpl(didResolverRegistry);
        context.registerService(DidPublicKeyResolver.class, publicKeyResolver);

        registerParsers(privateKeyResolver);
    }

    private void registerParsers(PrivateKeyResolver resolver) {

        // add EC-/PEM-Parser
        resolver.addParser(ECKey.class, encoded -> {
            try {
                return (ECKey) JWK.parseFromPEMEncodedObjects(encoded);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });
        resolver.addParser(PrivateKeyWrapper.class, encoded -> {
            try {
                var ecKey = (ECKey) JWK.parseFromPEMEncodedObjects(encoded);
                return new EcPrivateKeyWrapper(ecKey);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });

    }
}
