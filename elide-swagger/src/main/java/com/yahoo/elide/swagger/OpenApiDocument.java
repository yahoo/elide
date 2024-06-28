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
import io.swagger.v3.oas.models.SpecVersion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The Open API Document.
 */
public class OpenApiDocument {
    public static final String DEFAULT_TITLE = "Elide Service";

    /**
     * The OpenAPI media types.
     */
    public static class MediaType {
        private MediaType() {
        }

        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_YAML = "application/yaml";
    }

    private final Map<String, String> documents = new ConcurrentHashMap<>(2);
    private final Supplier<OpenAPI> openApi;

    public OpenApiDocument(OpenAPI openApi) {
        this(() -> openApi);
    }

    public OpenApiDocument(Supplier<OpenAPI> openApi) {
        this.openApi = openApi;
    }

    public String ofMediaType(String mediaType) {
        String key = mediaType;
        if (MediaType.APPLICATION_YAML.equalsIgnoreCase(key)) {
            key = MediaType.APPLICATION_YAML;
        } else {
            key = MediaType.APPLICATION_JSON;
        }
        return this.documents.computeIfAbsent(key, type -> of(this.openApi.get(), type));
    }

    /**
     * Converts a OpenAPI document to human-formatted JSON/YAML.
     *
     * @param openApi   OpenAPI document
     * @param mediaType Either application/json or application/yaml
     * @return Pretty printed 'OpenAPI' document in JSON.
     */
    public static String of(OpenAPI openApi, String mediaType) {
        if (MediaType.APPLICATION_YAML.equalsIgnoreCase(mediaType)) {
            if (SpecVersion.V31.equals(openApi.getSpecVersion())) {
                return Yaml31.pretty(openApi);
            }
            return Yaml.pretty(openApi);
        } else if (SpecVersion.V31.equals(openApi.getSpecVersion())) {
            return Json31.pretty(openApi);
        }
        return Json.pretty(openApi);
    }
}
