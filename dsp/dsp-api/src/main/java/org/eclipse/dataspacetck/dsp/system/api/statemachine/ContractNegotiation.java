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

package org.eclipse.dataspacetck.dsp.system.api.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.ACCEPTED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.AGREED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.FINALIZED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.OFFERED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.REQUESTED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.TERMINATED;
import static org.eclipse.dataspacetck.dsp.system.api.statemachine.ContractNegotiation.State.VERIFIED;

/**
 * The contract negotiation entity.
 * <p>
 * This implementation is thread-safe.
 */
public class ContractNegotiation {

    private static final Consumer<ContractNegotiation> NULL_WORK = n -> {
    };
    private final List<BiConsumer<State, ContractNegotiation>> listeners = new ArrayList<>();
    private final List<Map<String, Object>> offers = new ArrayList<>();
    private final LockManager lockManager = new LockManager();
    private String id;
    private String correlationId;
    private String offerId;
    private String datasetId;
    private String counterPartyId;
    private String callbackAddress;
    private State state = State.INITIALIZED;
    private Map<String, Object> agreement;
    private NegotiationKind negotiationKind = NegotiationKind.Consumer;

    private ContractNegotiation() {
    }

    public String getId() {
        return id;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    public String getCorrelationId() {
        return lockManager.readLock(() -> correlationId);
    }

    public State getState() {
        return lockManager.readLock(() -> state);
    }

    public Map<String, Object> getLastOffer() {
        return lockManager.readLock(() -> offers.isEmpty() ? null : offers.get(offers.size() - 1));
    }

    public String consumerPid() {
        return negotiationKind.equals(NegotiationKind.Consumer) ? id : correlationId;
    }

    public String providerPid() {
        return negotiationKind.equals(NegotiationKind.Provider) ? id : correlationId;
    }

    public List<Map<String, Object>> getOffers() {
        return lockManager.readLock(() -> new ArrayList<>(offers));
    }

    public Map<String, Object> getAgreement() {
        return lockManager.readLock(() -> agreement);
    }

    /**
     * Sets the correlation id.
     */
    public void setCorrelationId(String id, State state) {
        lockManager.writeLock(() -> {
            this.correlationId = id;
            transition(state);
            return null;
        });
    }

    /**
     * Stores the offer and executes the work while holding a write lock.
     */
    public void storeOffer(Map<String, Object> offer, State state, Consumer<ContractNegotiation> work) {
        lockManager.writeLock(() -> {
            offers.add(offer);
            transition(state);
            return null;
        });
        work.accept(this);
    }

    /**
     * Stores the agreement and executes the work while holding a write lock.
     */
    public void storeAgreement(Map<String, Object> agreement, Consumer<ContractNegotiation> work) {
        lockManager.writeLock(() -> {
            this.agreement = agreement;
            transition(AGREED);
            return null;
        });
        work.accept(this);
    }

    /**
     * Transitions to the new state.
     */
    public void transition(State newState) throws IllegalStateException {
        transition(newState, NULL_WORK);
    }

    /**
     * Transitions to the new state and executes the work while holding a write-lock.
     */
    public void transition(State newState, Consumer<ContractNegotiation> work) throws IllegalStateException {
        lockManager.writeLock(() -> {
            var oldState = state;
            switch (state) {
                case INITIALIZED -> {
                    assertStates(newState, REQUESTED, OFFERED, TERMINATED);
                    verifyCorrelationId(newState);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case REQUESTED -> {
                    assertStates(newState, OFFERED, AGREED, TERMINATED);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case OFFERED -> {
                    assertStates(newState, REQUESTED, OFFERED, ACCEPTED, TERMINATED);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case ACCEPTED -> {
                    assertStates(newState, AGREED, TERMINATED);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case AGREED -> {
                    assertStates(newState, VERIFIED, TERMINATED);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case VERIFIED -> {
                    assertStates(newState, FINALIZED, TERMINATED);
                    state = newState;
                    listeners.forEach(l -> l.accept(oldState, this));
                }
                case FINALIZED -> throw new IllegalStateException(FINALIZED + " is a final state");
                case TERMINATED -> throw new IllegalStateException(TERMINATED + " is a final state");
                default -> throw new IllegalStateException("Unexpected value: " + state);
            }
            work.accept(this);
            return null;
        });
    }

    private void verifyCorrelationId(State newState) {
        if (newState == REQUESTED || newState == OFFERED) {
            if (correlationId == null) {
                throw new IllegalStateException("Correlation id not set");
            }
        }
    }

    private void assertStates(State toState, State... states) {
        for (var state : states) {
            if (toState == state) {
                return;
            }
        }
        var legalStates = Arrays.stream(states).map(Enum::toString).collect(Collectors.joining(", "));
        throw new IllegalStateException(format("Illegal state transition from %s to %s. To state must be one of %s.", this.state, toState, legalStates));
    }

    public enum State {
        INITIALIZED,
        REQUESTED,
        OFFERED,
        ACCEPTED,
        AGREED,
        VERIFIED,
        FINALIZED,
        TERMINATED
    }

    public enum NegotiationKind {
        Consumer,
        Provider
    }

    public static class Builder {
        private ContractNegotiation negotiation;

        private Builder() {
            negotiation = new ContractNegotiation();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder correlationId(String correlationId) {
            negotiation.correlationId = correlationId;
            return this;
        }

        public Builder offerId(String offerId) {
            negotiation.offerId = offerId;
            return this;
        }

        public Builder datasetId(String datasetId) {
            negotiation.datasetId = datasetId;
            return this;
        }

        public Builder listener(BiConsumer<State, ContractNegotiation> listener) {
            negotiation.listeners.add(listener);
            return this;
        }

        public Builder state(State state) {
            this.negotiation.state = state;
            return this;
        }

        public Builder counterPartyId(String counterPartyId) {
            this.negotiation.counterPartyId = counterPartyId;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            requireNonNull(callbackAddress, "callbackAddress must not be null");
            this.negotiation.callbackAddress = callbackAddress.endsWith("/") ? callbackAddress.substring(0, callbackAddress.length() - 1) : callbackAddress;
            return this;
        }

        public Builder negotiationKind(NegotiationKind negotiationKind) {
            this.negotiation.negotiationKind = negotiationKind;
            return this;
        }

        public ContractNegotiation build() {
            negotiation.id = randomUUID().toString();
            negotiation.verifyCorrelationId(negotiation.state);
            Objects.requireNonNull(negotiation.negotiationKind);
            return negotiation;
        }

    }
}
