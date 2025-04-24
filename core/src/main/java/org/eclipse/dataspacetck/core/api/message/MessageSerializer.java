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

package org.eclipse.dataspacetck.core.api.message;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apicatalog.jsonld.JsonLd.compact;
import static com.apicatalog.jsonld.JsonLd.expand;

/**
 * Provides a configured {@link ObjectMapper} for serializing and deserializing JSON-LD messages.
 */
public class MessageSerializer {
    public static final JsonDocument EMPTY_CONTEXT = JsonDocument.of(JsonStructure.EMPTY_JSON_OBJECT);

    public static final ObjectMapper MAPPER;

    private static final Map<URI, Document> CONTEXTS;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        MAPPER.registerModule(module);
        CONTEXTS = new HashMap<>();

        registerDocument(URI.create("https://w3id.org/dspace/2025/1/context.jsonld"), "dsp-2025-1.jsonld");
        registerDocument(URI.create("https://w3id.org/dspace/2025/1/odrl-profile.jsonld"), "dsp-2025-1-odrl-profile.jsonld");
    }


    private MessageSerializer() {
    }

    static Document registerDocument(URI uri, String resource) {
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + resource);
        }
        try {
            return CONTEXTS.put(uri, JsonDocument.of(stream));
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(Object object) {
        try {
            var compacted = compact(JsonDocument.of(MAPPER.convertValue(object, JsonObject.class)), EMPTY_CONTEXT).get();
            return MAPPER.writeValueAsString(compacted);
        } catch (JsonProcessingException | JsonLdError e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializePlainJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> processJsonLd(InputStream stream, List<String> context, MessageValidator validator) {
        try {
            return processJsonLd(MAPPER.readValue(stream, JsonObject.class), context, validator);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> processJsonLd(InputStream stream, List<String> context) {
        try {
            return processJsonLd(MAPPER.readValue(stream, JsonObject.class), context, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> processJsonLd(Map<String, Object> message, List<String> context) {
        return processJsonLd(MAPPER.convertValue(message, JsonObject.class), context, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> processJsonLd(JsonObject document, List<String> context, MessageValidator validator) {
        try {

            if (validator != null) {
                var result = validator.validate(MAPPER.convertValue(document, JsonNode.class));
                if (!result.isValid()) {
                    throw new AssertionError("Invalid Json document: " + result.getMessage());
                }
            }
            var options = new JsonLdOptions((uri, documentLoaderOptions) -> CONTEXTS.get(uri));
            var ctx = Json.createObjectBuilder().add("@context", Json.createArrayBuilder(context)).build();
            options.setExpandContext(ctx);
            var jsonArray = expand(JsonDocument.of(document)).options(options).get();
            if (jsonArray.isEmpty()) {
                throw new AssertionError("Invalid Json document, expecting a non-empty array");
            }
            @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
            var expanded = jsonArray.get(0);
            return MAPPER.convertValue(expanded, Map.class);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }
    }


}
