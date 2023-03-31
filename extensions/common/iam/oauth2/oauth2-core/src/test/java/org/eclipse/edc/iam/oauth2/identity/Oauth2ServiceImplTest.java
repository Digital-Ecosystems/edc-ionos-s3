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
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.iam.oauth2.identity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.oauth2.Oauth2ServiceConfiguration;
import org.eclipse.edc.iam.oauth2.rule.Oauth2ValidationRulesRegistryImpl;
import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.PrivateKeyOauth2CredentialsRequest;
import org.eclipse.edc.jwt.JwtDecoratorRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Oauth2ServiceImplTest {

    private static final String CLIENT_ID = "client-test";
    private static final String PRIVATE_KEY_ALIAS = "pk-test";
    private static final String PUBLIC_CERTIFICATE_ALIAS = "cert-test";
    private static final String PROVIDER_AUDIENCE = "audience-test";
    private static final String ENDPOINT_AUDIENCE = "endpoint-audience-test";
    private static final String OAUTH2_SERVER_URL = "http://oauth2-server.com";

    private final Instant now = Instant.now();
    private final Oauth2Client client = mock(Oauth2Client.class);
    private final TokenGenerationService tokenGenerationService = mock(TokenGenerationService.class);
    private final CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider = mock(CredentialsRequestAdditionalParametersProvider.class);
    private Oauth2ServiceImpl authService;
    private JWSSigner jwsSigner;

    @BeforeEach
    void setUp() throws JOSEException {
        var testKey = testKey();

        jwsSigner = new RSASSASigner(testKey.toPrivateKey());
        var publicKeyResolverMock = mock(PublicKeyResolver.class);
        var privateKeyResolverMock = mock(PrivateKeyResolver.class);
        var certificateResolverMock = mock(CertificateResolver.class);
        when(publicKeyResolverMock.resolveKey(anyString())).thenReturn(testKey.toPublicKey());
        var configuration = Oauth2ServiceConfiguration.Builder.newInstance()
                .tokenUrl(OAUTH2_SERVER_URL)
                .clientId(CLIENT_ID)
                .privateKeyAlias(PRIVATE_KEY_ALIAS)
                .publicCertificateAlias(PUBLIC_CERTIFICATE_ALIAS)
                .providerAudience(PROVIDER_AUDIENCE)
                .endpointAudience(ENDPOINT_AUDIENCE)
                .privateKeyResolver(privateKeyResolverMock)
                .certificateResolver(certificateResolverMock)
                .identityProviderKeyResolver(publicKeyResolverMock)
                .build();

        var clock = Clock.fixed(now, UTC);
        var validationRulesRegistry = new Oauth2ValidationRulesRegistryImpl(configuration, clock);
        var tokenValidationService = new TokenValidationServiceImpl(publicKeyResolverMock, validationRulesRegistry);

        authService = new Oauth2ServiceImpl(configuration, tokenGenerationService, client, new JwtDecoratorRegistryImpl(), tokenValidationService, credentialsRequestAdditionalParametersProvider);
    }

    @Test
    void obtainClientCredentials() {
        when(credentialsRequestAdditionalParametersProvider.provide(any())).thenReturn(emptyMap());
        when(tokenGenerationService.generate(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("assertionToken").build()));

        var tokenParameters = TokenParameters.Builder.newInstance()
                .audience("audience")
                .scope("scope")
                .build();

        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("accessToken").build()));

        var result = authService.obtainClientCredentials(tokenParameters);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("accessToken");
        var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
        verify(client).requestToken(captor.capture());
        var captured = captor.getValue();
        assertThat(captured).isNotNull()
                .isInstanceOf(PrivateKeyOauth2CredentialsRequest.class);
        var capturedRequest = (PrivateKeyOauth2CredentialsRequest) captured;
        assertThat(capturedRequest.getGrantType()).isEqualTo("client_credentials");
        assertThat(capturedRequest.getScope()).isEqualTo("scope");
        assertThat(capturedRequest.getClientAssertion()).isEqualTo("assertionToken");
        assertThat(capturedRequest.getClientAssertionType()).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
    }

    @Test
    void obtainClientCredentials_addsAdditionalFormParameters() {
        when(credentialsRequestAdditionalParametersProvider.provide(any())).thenReturn(Map.of("parameterKey", "parameterValue"));
        when(tokenGenerationService.generate(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("assertionToken").build()));

        var tokenParameters = TokenParameters.Builder.newInstance()
                .audience("audience")
                .scope("scope")
                .build();

        when(client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("accessToken").build()));

        var result = authService.obtainClientCredentials(tokenParameters);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("accessToken");
        var captor = ArgumentCaptor.forClass(Oauth2CredentialsRequest.class);
        verify(client).requestToken(captor.capture());
        var captured = captor.getValue();
        assertThat(captured).isNotNull()
                .isInstanceOf(PrivateKeyOauth2CredentialsRequest.class);
        var capturedRequest = (PrivateKeyOauth2CredentialsRequest) captured;
        assertThat(capturedRequest.getGrantType()).isEqualTo("client_credentials");
        assertThat(capturedRequest.getScope()).isEqualTo("scope");
        assertThat(capturedRequest.getClientAssertion()).isEqualTo("assertionToken");
        assertThat(capturedRequest.getParams()).containsEntry("parameterKey", "parameterValue");
    }

    @Test
    void obtainClientCredentials_verifyReturnsFailureIfOauth2ClientFails() {
        when(credentialsRequestAdditionalParametersProvider.provide(any())).thenReturn(emptyMap());
        when(tokenGenerationService.generate(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("assertionToken").build()));

        var tokenParameters = TokenParameters.Builder.newInstance()
                .audience("audience")
                .scope("scope")
                .build();

        when(client.requestToken(any())).thenReturn(Result.failure("test error"));

        var result = authService.obtainClientCredentials(tokenParameters);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("test error");
    }

    @Test
    void verifyNoAudienceToken() {
        var jwt = createJwt(null, Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, ENDPOINT_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAudienceToken() {
        var jwt = createJwt("different.audience", Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, ENDPOINT_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAttemptUseNotBeforeToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.plusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, ENDPOINT_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyExpiredToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.minusSeconds(1000)), Date.from(now.minusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt, ENDPOINT_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyValidJwt() {
        var jwt = createJwt(ENDPOINT_AUDIENCE, Date.from(now.minusSeconds(1000)), new Date(System.currentTimeMillis() + 1000000));

        var result = authService.verifyJwtToken(jwt, ENDPOINT_AUDIENCE);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims()).hasSize(3).containsKeys(AUDIENCE, NOT_BEFORE, EXPIRATION_TIME);
    }

    private PrivateKeyOauth2CredentialsRequest createRequest(Map<String, String> additional) {
        return PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                .grantType("client_credentials")
                .clientAssertion("assertion-token")
                .scope("scope")
                .params(additional)
                .build();
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private TokenRepresentation createJwt(String aud, Date nbf, Date exp) {
        var claimsSet = new JWTClaimsSet.Builder()
                .audience(aud)
                .notBeforeTime(nbf)
                .expirationTime(exp).build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("an-id").build();

        try {
            var jwt = new SignedJWT(header, claimsSet);
            jwt.sign(jwsSigner);
            return TokenRepresentation.Builder.newInstance().token(jwt.serialize()).build();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }
}
