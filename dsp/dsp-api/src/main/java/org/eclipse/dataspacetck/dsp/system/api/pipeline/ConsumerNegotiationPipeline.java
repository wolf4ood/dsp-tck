/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.dataspacetck.dsp.system.api.pipeline;

import org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Constructs a contract negotiation with a consumer connector under test. Uses a TCK provider connector to interact
 * with the consumer connector being verified.
 */
public interface ConsumerNegotiationPipeline extends NegotiationPipeline<ConsumerNegotiationPipeline> {

    /**
     * Initiates a request with the consumer connector being verified. Consumer connectors are required to expose an HTTP GET
     * endpoint to initiate requests.
     */
    ConsumerNegotiationPipeline initiateRequest(String datasetId, String offerId);

    /**
     * Sends an offer to the consumer connector being verified.
     */
    default ConsumerNegotiationPipeline sendOfferMessage() {
        return sendOfferMessage(false);
    }

    /**
     * Sends an offer to the consumer connector being verified.
     */
    ConsumerNegotiationPipeline sendOfferMessage(boolean expectError);

    /**
     * Sends an agreement to the consumer connector being verified.
     */
    default ConsumerNegotiationPipeline sendAgreementMessage() {
        return sendAgreementMessage(false);
    }

    /**
     * Sends an agreement to the consumer connector being verified.
     */
    ConsumerNegotiationPipeline sendAgreementMessage(boolean expectError);

    /**
     * Sends a finalized event to the consumer connector being verified.
     */
    default ConsumerNegotiationPipeline sendFinalizedEvent() {
        return sendFinalizedEvent(false);
    }

    /**
     * Sends a finalized event to the consumer connector being verified.
     */
    ConsumerNegotiationPipeline sendFinalizedEvent(boolean expectError);

    /**
     * Expect an init request and execute the given action.
     */
    ConsumerNegotiationPipeline expectInitialRequest(BiFunction<Map<String, Object>, String, Map<String, Object>> action);

    /**
     * Expect a request and execute the given action.
     */
    ConsumerNegotiationPipeline expectRequest(BiFunction<Map<String, Object>, String, Map<String, Object>> action);

    /**
     * Expect an accepted event to be received and execute the given action.
     */
    ConsumerNegotiationPipeline expectAcceptedEvent(Consumer<Map<String, Object>> action);

    /**
     * Expect a verification message to be received and execute the given action.
     */
    ConsumerNegotiationPipeline expectVerifiedMessage(Consumer<Map<String, Object>> action);

    /**
     * Verifies the active consumer contract negotiation is in the given state.
     */
    ConsumerNegotiationPipeline thenVerifyConsumerState(State state);
}
