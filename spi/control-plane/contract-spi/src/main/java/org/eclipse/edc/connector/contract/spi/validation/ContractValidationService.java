/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add policy scope
 *
 */

package org.eclipse.edc.connector.contract.spi.validation;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Validates {@link ContractOffer}s and {@link ContractAgreement}s.
 */
@ExtensionPoint
public interface ContractValidationService {

    @PolicyScope
    String NEGOTIATION_SCOPE = "contract.negotiation";

    /**
     * Validates and sanitizes the contract offer for the consumer represented by the given claims.
     * <p>
     * The original offer must be validated and sanitized to avoid policy and asset injection attacks by malicious consumers.
     *
     * @param token The {@link ClaimToken} of the consumer
     * @param offer The initial {@link ContractOffer} to validate
     * @return the sanitized version {@link ContractOffer}. The input {@link ContractOffer} could contain some fields or values
     *         on policy or asset that differs from the original policy and asset defined by the provider, which could cause injection attacks.
     *         The provider validation should return a new {@link ContractOffer} that contains the original policy and asset defined by the provider.
     */
    @NotNull
    Result<ContractOffer> validateInitialOffer(ClaimToken token, ContractOffer offer);

    /**
     * Validates the contract agreement that the consumer referenced in its transfer request.
     * The {@code ClaimToken} must represent the same counter-party, that is referenced in the contract agreement.
     *
     * @param token The {@link ClaimToken} of the consumer
     * @param agreement The {@link ContractAgreement} between consumer and provider to validate
     * @return the result of the validation
     */
    Result<ContractAgreement> validateAgreement(ClaimToken token, ContractAgreement agreement);

    /**
     * When the negotiation has been confirmed by the provider, the consumer must validate it ensuring that is the same
     * one that was sent in the last offer
     *
     * @param agreement The {@link ContractAgreement} between consumer and provider
     * @param latestOffer The last {@link ContractOffer}
     */
    Result<Void> validateConfirmed(ContractAgreement agreement, ContractOffer latestOffer);

}
