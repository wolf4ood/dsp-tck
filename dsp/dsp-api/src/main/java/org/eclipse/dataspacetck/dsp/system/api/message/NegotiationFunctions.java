/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 *
 */

package org.eclipse.dataspacetck.dsp.system.api.message;

import org.eclipse.dataspacetck.dsp.system.api.metadata.DspTestingWorkaround;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.CONTEXT;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_AGREEMENT;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_CODE;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_EVENT_TYPE;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_OFFER;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_REASON;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_STATE;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.DSPACE_PROPERTY_TIMESTAMP;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.ID;
import static org.eclipse.dataspacetck.dsp.system.api.message.DspConstants.TYPE;
import static org.eclipse.dataspacetck.dsp.system.api.message.JsonLdFunctions.createDspContext;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_AGREEMENT_TYPE;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_OFFER_TYPE;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_ACTION;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_ASSIGNEE;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_ASSIGNER;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_CONSTRAINTS;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_PERMISSION;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_PROPERTY_TARGET;
import static org.eclipse.dataspacetck.dsp.system.api.message.OdrlConstants.ODRL_USE;

/**
 * Utility methods for creating DSP messages.
 */
public class NegotiationFunctions {


    private NegotiationFunctions() {
    }

    public static Map<String, Object> createTermination(String providerId, String consumerId, String code, String... reasons) {
        var message = createBaseMessage("ContractNegotiationTerminationMessage");
        message.put(CONTEXT, createDspContext());

        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);
        message.put(DSPACE_PROPERTY_CODE, code);

        if (reasons != null && reasons.length > 0) {
            message.put(DSPACE_PROPERTY_REASON, Arrays.stream(reasons).map(reason -> Map.of("message", reason)).collect(toList()));
        }
        return message;
    }

    public static Map<String, Object> createAcceptedEvent(String processId, String consumerId) {
        return createEvent(processId, consumerId, "ACCEPTED");
    }

    public static Map<String, Object> createFinalizedEvent(String processId, String consumerId) {
        return createEvent(processId, consumerId, "FINALIZED");
    }

    public static Map<String, Object> createEvent(String providerId, String consumerId, String eventType) {
        var message = createBaseMessage("ContractNegotiationEventMessage");
        message.put(CONTEXT, createDspContext());
        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);

        message.put(DSPACE_PROPERTY_EVENT_TYPE, eventType);
        return message;
    }

    public static Map<String, Object> createContractRequest(String consumerPid, String offerId, String targetId, String callbackAddress) {
        return createContractRequest(consumerPid, null, offerId, targetId, callbackAddress);
    }

    public static Map<String, Object> createContractRequest(String consumerPid, String providerPid, String offerId, String targetId, String callbackAddress) {
        var message = createBaseMessage("ContractRequestMessage");
        message.put(CONTEXT, createDspContext());
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerPid);

        if (providerPid != null) {
            message.put(DSPACE_PROPERTY_PROVIDER_PID, providerPid);
        }

        @DspTestingWorkaround("Remove @type")
        var offer = new LinkedHashMap<String, Object>();
        offer.put(ID, offerId);
        offer.put(ODRL_PROPERTY_TARGET, targetId);
        offer.put(TYPE, ODRL_OFFER_TYPE);    // WORKAROUND: REMOVE - @type
        var permissions = Map.of(ODRL_PROPERTY_ACTION, ODRL_USE, ODRL_PROPERTY_CONSTRAINTS, emptyList());
        offer.put(ODRL_PROPERTY_PERMISSION, List.of(permissions));

        message.put(DSPACE_PROPERTY_OFFER, offer);

        if (callbackAddress != null) {
            message.put(DSPACE_PROPERTY_CALLBACK_ADDRESS, callbackAddress);
        }

        return message;
    }

    public static Map<String, Object> createCounterOffer(String providerId,
                                                         String consumerId,
                                                         String offerId,
                                                         String assigner,
                                                         String assignee,
                                                         String targetId) {
        var message = createBaseMessage("ContractRequestMessage"); // do NOT override id
        message.put(CONTEXT, createDspContext());
        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);

        message.put(DSPACE_PROPERTY_OFFER, createOfferPolicy(offerId, assigner, assignee, targetId));

        return message;
    }

    public static Map<String, Object> createVerification(String providerId, String consumerId) {
        var message = createBaseMessage("ContractAgreementVerificationMessage");
        message.put(CONTEXT, createDspContext());

        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);
        return message;
    }

    public static Map<String, Object> createOffer(String providerId,
                                                  String consumerId,
                                                  String offerId,
                                                  String assigner,
                                                  String assignee,
                                                  String targetId) {
        var message = createBaseMessage("ContractOfferMessage");
        var context = createDspContext();
        message.put(CONTEXT, context);
        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);

        var offer = createOfferPolicy(offerId, assigner, assignee, targetId);

        message.put(DSPACE_PROPERTY_OFFER, offer);

        return message;
    }

    @NotNull
    private static LinkedHashMap<String, Object> createOfferPolicy(String offerId,
                                                                   String assigner,
                                                                   String assignee,
                                                                   String targetId) {
        @DspTestingWorkaround("Remove @type")
        var offer = new LinkedHashMap<String, Object>();
        offer.put(TYPE, ODRL_OFFER_TYPE);
        offer.put(ID, offerId);
        var permissions = Map.of(ODRL_PROPERTY_ACTION, ODRL_USE, ODRL_PROPERTY_CONSTRAINTS, emptyList());
        offer.put(ODRL_PROPERTY_PERMISSION, List.of(permissions));
        offer.put(ODRL_PROPERTY_TARGET, targetId);
        offer.put(ODRL_PROPERTY_ASSIGNEE, assignee);
        offer.put(ODRL_PROPERTY_ASSIGNER, assigner);

        return offer;
    }

    public static Map<String, Object> createAgreement(String providerId,
                                                      String consumerId,
                                                      String agreementId,
                                                      String assigner,
                                                      String assignee,
                                                      String targetId,
                                                      String callbackAddress) {
        var message = createBaseMessage("ContractAgreementMessage");
        var context = createDspContext();
        message.put(CONTEXT, context);
        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerId);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerId);

        var permissions = Map.of(ODRL_PROPERTY_ACTION, ODRL_USE, ODRL_PROPERTY_CONSTRAINTS, emptyList());
        var offer = new LinkedHashMap<String, Object>();
        offer.put(TYPE, ODRL_AGREEMENT_TYPE);
        offer.put(ID, agreementId);
        offer.put(ODRL_PROPERTY_TARGET, targetId);
        offer.put(ODRL_PROPERTY_PERMISSION, List.of(permissions));
        offer.put(DSPACE_PROPERTY_TIMESTAMP, ZonedDateTime.now().format(ISO_INSTANT));
        offer.put(ODRL_PROPERTY_ASSIGNEE, assignee);
        offer.put(ODRL_PROPERTY_ASSIGNER, assigner);

        message.put(DSPACE_PROPERTY_AGREEMENT, offer);
        message.put(DSPACE_PROPERTY_CALLBACK_ADDRESS, callbackAddress);

        return message;
    }

    public static Map<String, Object> createNegotiationResponse(String providerPid, String consumerPid, String state) {
        var message = createBaseMessage("ContractNegotiation");
        var context = createDspContext();
        message.put(CONTEXT, context);
        message.put(DSPACE_PROPERTY_PROVIDER_PID, providerPid);
        message.put(DSPACE_PROPERTY_CONSUMER_PID, consumerPid);
        message.put(DSPACE_PROPERTY_STATE, state);
        return message;
    }


    @NotNull
    private static Map<String, Object> createBaseMessage(String type) {
        var message = new LinkedHashMap<String, Object>();
        message.put(ID, UUID.randomUUID().toString());
        message.put(TYPE, type);
        return message;
    }

}
