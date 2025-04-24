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

package org.eclipse.dataspacetck.core.system;

import org.eclipse.dataspacetck.core.api.system.CallbackEndpoint;
import org.eclipse.dataspacetck.core.api.system.HandlerResponse;
import org.eclipse.dataspacetck.core.api.system.ProtocolHandler;
import org.eclipse.dataspacetck.core.spi.boot.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;

/**
 * Implements a callback endpoint.
 * <p>
 * Deserialized messages from incoming transports such as HTTP are dispatched to a registered handler through this endpoint by calling {@link #apply(String, InputStream)}.
 */
public class DefaultCallbackEndpoint implements CallbackEndpoint, BiFunction<String, InputStream, String>, ExtensionContext.Store.CloseableResource {

    private final List<LifecycleListener> listeners = new ArrayList<>();
    private final Map<String, ProtocolHandler> handlers = new HashMap<>();
    private String address;
    private Monitor monitor;

    private DefaultCallbackEndpoint() {
    }

    @Override
    public String getAddress() {
        return address;
    }

    public boolean handlesPath(String path) {
        return lookupHandler(stripTrailingSlash(path)).isPresent();
    }

    @Override
    public String apply(String path, InputStream message) {
        //noinspection OptionalGetWithoutIsPresent
        return lookupHandler(stripTrailingSlash(path)).get().apply(emptyMap(), message).result();
    }

    public HandlerResponse apply(String path, Map<String, List<String>> headers, InputStream message) {
        //noinspection OptionalGetWithoutIsPresent
        return lookupHandler(stripTrailingSlash(path)).get().apply(headers, message);
    }

    @Override
    public void registerProtocolHandler(String path, ProtocolHandler handler) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = stripTrailingSlash(path);
        handlers.put(path, handler);
    }

    @Override
    public void registerHandler(String path, Function<InputStream, String> handler) {
        registerProtocolHandler(path, new DelegatingProtocolHandler(handler, monitor));
    }

    @Override
    public void deregisterHandler(String path) {
        handlers.remove(path);
    }

    @Override
    public void close() {
        listeners.forEach(l -> l.onClose(this));
    }

    @NotNull
    private String stripTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Matches the path based on the regular expression.
     */
    private Optional<ProtocolHandler> lookupHandler(String expression) {
        return handlers.entrySet()
                .stream()
                .filter(entry -> compile(entry.getKey()).matcher(expression).matches())
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @FunctionalInterface
    public interface LifecycleListener {
        void onClose(DefaultCallbackEndpoint endpoint);
    }

    public static class Builder {
        private final DefaultCallbackEndpoint endpoint;

        private Builder() {
            endpoint = new DefaultCallbackEndpoint();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder address(String address) {
            endpoint.address = address;
            return this;
        }

        public Builder listener(LifecycleListener listener) {
            endpoint.listeners.add(listener);
            return this;
        }

        public Builder monitor(Monitor monitor) {
            endpoint.monitor = monitor;
            return this;
        }

        public DefaultCallbackEndpoint build() {
            requireNonNull(endpoint.address);
            return endpoint;
        }

    }

    private static class DelegatingProtocolHandler implements ProtocolHandler {
        private final Function<InputStream, String> handler;
        private final Monitor m;

        DelegatingProtocolHandler(Function<InputStream, String> handler, Monitor monitor) {
            this.handler = requireNonNull(handler);
            m = monitor;
        }

        @Override
        public HandlerResponse apply(Map<String, List<String>> headers, InputStream body) {
            try {
                return new HandlerResponse(200, handler.apply(body));
            } catch (Throwable e) {
                m.debug(format("Error processing message %s", e.getMessage()));
                return new HandlerResponse(400, e.getMessage());
            }
        }
    }
}
