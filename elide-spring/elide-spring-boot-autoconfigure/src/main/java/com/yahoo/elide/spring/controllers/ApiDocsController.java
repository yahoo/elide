/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.swagger.OpenApiDocument;
import com.yahoo.elide.swagger.OpenApiDocument.MediaType;
import com.yahoo.elide.utils.HeaderUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Spring REST controller for exposing OpenAPI documentation.
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.api-docs.path}")
public class ApiDocsController {

    // Maps api version & path to OpenAPI document.
    protected Map<Pair<String, String>, OpenApiDocument> documents;

    /**
     * Wraps a list of open api registrations so that they can be wrapped with an
     * AOP proxy.
     */
    @Data
    @AllArgsConstructor
    public static class ApiDocsRegistrations {

        public ApiDocsRegistrations(Supplier<OpenAPI> doc, String version, String apiVersion) {
            registrations = List.of(new ApiDocsRegistration("", doc, version, apiVersion));
        }

        List<ApiDocsRegistration> registrations;
    }

    @Data
    @AllArgsConstructor
    public static class ApiDocsRegistration {
        private String path;
        private Supplier<OpenAPI> document;

        /**
         * The OpenAPI Specification Version.
         */
        private String version;

        /**
         * The API version.
         */
        private String apiVersion;
    }

    /**
     * Constructs the resource.
     *
     * @param docs A list of documents to register.
     */
    public ApiDocsController(ApiDocsRegistrations docs) {
        log.debug("Started ~~");
        documents = new HashMap<>();

        docs.getRegistrations().forEach(doc -> {
            String apiVersion = doc.getApiVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;

            documents.put(Pair.of(apiVersion, apiPath),
                    new OpenApiDocument(doc.document, OpenApiDocument.Version.from(doc.version)));
        });
    }

    @GetMapping(value = { "/", "" }, produces = MediaType.APPLICATION_JSON)
    public Callable<ResponseEntity<String>> listJson(@RequestHeader HttpHeaders requestHeaders) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        return list(apiVersion, MediaType.APPLICATION_JSON);
    }

    @GetMapping(value = { "/", "" }, produces = MediaType.APPLICATION_YAML)
    public Callable<ResponseEntity<String>> listYaml(@RequestHeader HttpHeaders requestHeaders) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        return list(apiVersion, MediaType.APPLICATION_YAML);
    }

    public Callable<ResponseEntity<String>> list(String apiVersion, String mediaType) {
        final List<String> documentPaths = documents.keySet().stream().filter(key -> key.getLeft().equals(apiVersion))
                .map(Pair::getRight).toList();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                if (documentPaths.size() == 1) {
                    Optional<Pair<String, String>> pair = documents.keySet().stream()
                            .filter(key -> key.getLeft().equals(apiVersion)).findFirst();
                    if (pair.isPresent()) {
                        return ResponseEntity.status(HttpStatus.OK)
                                .body(documents.get(pair.get()).ofMediaType(mediaType));
                    }
                }

                String body = documentPaths.stream().map(key -> '"' + key + '"')
                        .collect(Collectors.joining(",", "[", "]"));

                return ResponseEntity.status(HttpStatus.OK).body(body);
            }
        };
    }

    /**
     * Read handler.
     *
     * @param requestHeaders request headers
     * @param name           document name
     * @return response The OpenAPI JSON document
     */
    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON)
    public Callable<ResponseEntity<String>> listJson(@RequestHeader HttpHeaders requestHeaders,
            @PathVariable("name") String name) {

        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        return list(apiVersion, name, MediaType.APPLICATION_JSON);
    }

    /**
     * Read handler.
     *
     * @param requestHeaders request headers
     * @param name           document name
     * @return response The OpenAPI YAML document
     */
    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_YAML)
    public Callable<ResponseEntity<String>> listYaml(@RequestHeader HttpHeaders requestHeaders,
            @PathVariable("name") String name) {

        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        return list(apiVersion, name, MediaType.APPLICATION_YAML);
    }

    public Callable<ResponseEntity<String>> list(String apiVersion, String name, String mediaType) {
        final String encodedName = Encode.forHtml(name);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                Pair<String, String> lookupKey = Pair.of(apiVersion, encodedName);
                if (documents.containsKey(lookupKey)) {
                    return ResponseEntity.status(HttpStatus.OK).body(documents.get(lookupKey).ofMediaType(mediaType));
                }
                return ResponseEntity.status(404).body("Unknown document: " + encodedName);
            }
        };
    }
}
