/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.edc.protocol.ids.token.validation.rule;

import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;


/**
 * Validates the JWT by checking extended IDS rules.
 */
public class IdsValidationRule implements TokenValidationRule {
    private final boolean validateReferring;

    public IdsValidationRule(boolean validateReferring) {
        this.validateReferring = validateReferring;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (additional != null) {
            var issuerConnector = getString(additional, "issuerConnector");
            if (issuerConnector == null) {
                return Result.failure("Required issuerConnector is missing in message");
            }

            var securityProfile = getString(additional, "securityProfile");

            return verifyTokenIds(toVerify, issuerConnector, securityProfile);

        } else {
            throw new EdcException("Missing required additional information for IDS token validation");
        }
    }

    private Result<Void> verifyTokenIds(ClaimToken jwt, String issuerConnector, @Nullable String securityProfile) {
        //referringConnector (DAT, optional) vs issuerConnector (Message-Header, mandatory)
        var referringConnector = jwt.getClaim("referringConnector");

        if (validateReferring && !issuerConnector.equals(referringConnector)) {
            return Result.failure("refferingConnector in token does not match issuerConnector in message");
        }

        //securityProfile (DAT, mandatory) vs securityProfile (Message-Payload, optional)
        try {
            var tokenSecurityProfile = jwt.getClaim("securityProfile");

            if (securityProfile != null && !securityProfile.equals(tokenSecurityProfile)) {
                return Result.failure("securityProfile in token does not match securityProfile in payload");
            }
        } catch (Exception e) {
            //Nothing to do, payload mostly no connector instance
        }

        return Result.success();
    }

    @Nullable
    private String getString(@NotNull Map<String, Object> map, String key) {
        return Optional.of(map).map(it -> it.get(key)).map(Object::toString).orElse(null);
    }
}
