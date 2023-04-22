/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Open API Document.
 */
public class OpenApiDocument {
    /**
     * The OpenAPI media types.
     */
    public static class MediaType {
        private MediaType() {
        }

        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_YAML = "application/yaml";
    }

    /**
     * The OpenAPI version.
     */
    public enum Version {
        OPENAPI_3_0("3.0"), OPENAPI_3_1("3.1");

        private final String value;

        Version(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public static Version from(String version) {
            if (version.startsWith(OPENAPI_3_1.getValue())) {
                return OPENAPI_3_1;
            } else if (version.startsWith(OPENAPI_3_0.getValue())) {
                return OPENAPI_3_0;
            }
            throw new IllegalArgumentException("Invalid OpenAPI version. Only versions 3.0 and 3.1 are supported.");
        }
    }

    private final Map<String, String> documents = new ConcurrentHashMap<>(2);
    private final OpenAPI openApi;
    private final Version version;

    public OpenApiDocument(OpenAPI openApi, Version version) {
        this.openApi = openApi;
        this.version = version;
    }

    public String ofMediaType(String mediaType) {
        String key = mediaType;
        if (MediaType.APPLICATION_YAML.equalsIgnoreCase(key)) {
            key = MediaType.APPLICATION_YAML;
        } else {
            key = MediaType.APPLICATION_JSON;
        }
        return this.documents.computeIfAbsent(key, type -> of(this.openApi, this.version, type));
    }

    /**
     * Converts a OpenAPI document to human-formatted JSON/YAML.
     *
     * @param openApi   OpenAPI document
     * @param version   OpenAPI version
     * @param mediaType Either application/json or application/yaml
     * @return Pretty printed 'OpenAPI' document in JSON.
     */
    public static String of(OpenAPI openApi, Version version, String mediaType) {
        if (MediaType.APPLICATION_YAML.equalsIgnoreCase(mediaType)) {
            if (Version.OPENAPI_3_1.equals(version)) {
                return Yaml31.pretty(openApi);
            }
            return Yaml.pretty(openApi);
        } else if (Version.OPENAPI_3_1.equals(version)) {
            return Json31.pretty(openApi);
        }
        return Json.pretty(openApi);
    }
}
