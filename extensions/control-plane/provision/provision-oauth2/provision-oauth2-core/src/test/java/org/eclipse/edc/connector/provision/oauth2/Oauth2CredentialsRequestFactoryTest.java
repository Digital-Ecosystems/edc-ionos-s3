/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.provision.oauth2;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.PRIVATE_KEY_NAME;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.SCOPE;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.TOKEN_URL;
import static org.eclipse.edc.connector.provision.oauth2.Oauth2DataAddressSchema.VALIDITY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Oauth2CredentialsRequestFactoryTest {

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private final PrivateKeyResolver privateKeyResolver = mock(PrivateKeyResolver.class);
    private final Oauth2CredentialsRequestFactory factory = new Oauth2CredentialsRequestFactory(privateKeyResolver, clock);

    @Test
    void shouldCreateSharedSecretRequest_whenPrivateKeyNameIsAbsent() {
        var address = defaultAddress()
                .property(CLIENT_SECRET, "clientSecret")
                .property(SCOPE, "scope")
                .build();

        var result = factory.create(createResourceDefinition(address));

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent)
                .asInstanceOf(type(SharedSecretOauth2CredentialsRequest.class))
                .satisfies(request -> {
                    assertThat(request.getGrantType()).isEqualTo("client_credentials");
                    assertThat(request.getClientId()).isEqualTo("clientId");
                    assertThat(request.getClientSecret()).isEqualTo("clientSecret");
                    assertThat(request.getUrl()).isEqualTo("http://oauth2-server.com/token");
                    assertThat(request.getScope()).isEqualTo("scope");
                });
        verifyNoInteractions(privateKeyResolver);
    }

    @Test
    void shouldCreatePrivateKeyRequest_whenPrivateKeyNameIsPresent() throws JOSEException {
        var keyPair = generateKeyPair();
        when(privateKeyResolver.resolvePrivateKey("pk-test", PrivateKey.class)).thenReturn(keyPair.toPrivateKey());

        var address = defaultAddress()
                .property(PRIVATE_KEY_NAME, "pk-test")
                .property(VALIDITY, "600")
                .build();

        var result = factory.create(createResourceDefinition(address));

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent)
                .asInstanceOf(type(PrivateKeyOauth2CredentialsRequest.class))
                .satisfies(request -> {
                    assertThat(request.getGrantType()).isEqualTo("client_credentials");
                    assertThat(request.getUrl()).isEqualTo("http://oauth2-server.com/token");
                    assertThat(request.getClientAssertionType()).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
                    assertThat(request.getScope()).isEqualTo(null);
                })
                .extracting(PrivateKeyOauth2CredentialsRequest::getClientAssertion)
                .satisfies(assertion -> {
                    var assertionToken = SignedJWT.parse(assertion);
                    var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
                    assertThat(assertionToken.verify(new RSASSAVerifier(keyPair.toRSAPublicKey()))).isTrue();
                    assertThat(assertionToken.getJWTClaimsSet().getClaims())
                            .hasFieldOrPropertyWithValue("sub", "clientId")
                            .hasFieldOrPropertyWithValue("iss", "clientId")
                            .hasFieldOrPropertyWithValue("aud", List.of(address.getProperty(TOKEN_URL)))
                            .hasFieldOrProperty("jti")
                            .hasFieldOrPropertyWithValue("iat", Date.from(now))
                            .hasFieldOrPropertyWithValue("exp", Date.from(now.plusSeconds(600)));
                });
    }

    @Test
    void shouldFailIfPrivateKeySecretNotFound() {
        when(privateKeyResolver.resolvePrivateKey("pk-test", PrivateKey.class)).thenReturn(null);

        var address = defaultAddress()
                .property(PRIVATE_KEY_NAME, "pk-test")
                .property(VALIDITY, "600")
                .build();

        var result = factory.create(createResourceDefinition(address));

        assertThat(result).matches(Result::failed)
                .extracting(Result::getFailureDetail).asString().contains("pk-test");
    }

    private Oauth2ResourceDefinition createResourceDefinition(DataAddress address) {
        return Oauth2ResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .dataAddress(address)
                .build();
    }

    private HttpDataAddress.Builder defaultAddress() {
        return HttpDataAddress.Builder.newInstance()
                .property(CLIENT_ID, "clientId")
                .property(TOKEN_URL, "http://oauth2-server.com/token");
    }

    private RSAKey generateKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

}
