/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import com.yahoo.elide.contrib.swagger.SwaggerBuilder;

import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.models.Swagger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Spring REST controller for exposing Swagger documentation.
 */
@Slf4j
@RestController
@Configuration
@RequestMapping(value = "${elide.swagger.path}")
@ConditionalOnExpression("${elide.swagger.enabled:false}")
public class SwaggerController {

    //Maps api version & path to swagger document.
    protected Map<Pair<String, String>, String> documents;
    private static final String JSON_CONTENT_TYPE = "application/json";

    @Data
    @AllArgsConstructor
    public static class SwaggerRegistration {
        private String path;
        private Swagger document;
    }

    /**
     * Constructs the resource.
     *
     * @param docs A list of documents to register.
     */
    @Autowired(required = false)
    public SwaggerController(List<SwaggerRegistration> docs) {
        log.debug("Started ~~");
        documents = new HashMap<>();

        docs.forEach((doc) -> {
            String apiVersion = doc.document.getInfo().getVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;

            documents.put(Pair.of(apiVersion, apiPath), SwaggerBuilder.getDocument(doc.document));
        });
    }

    @Autowired(required = false)
    public SwaggerController(Swagger doc) {
        log.debug("Started ~~");
        documents = new HashMap<>();

        documents.put(Pair.of(NO_VERSION, ""), SwaggerBuilder.getDocument(doc));
    }

    @GetMapping(value = {"/", ""}, produces = JSON_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> list(@RequestHeader Map<String, String> requestHeaders) {
        final String apiVersion = Utils.getApiVersion(requestHeaders);

        final List<String> documentPaths = documents.keySet().stream()
                .filter(key -> key.getLeft().equals(apiVersion))
                .map(key -> key.getRight())
                .collect(Collectors.toList());

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                if (documentPaths.size() == 1) {
                    return ResponseEntity
                            .status(HttpStatus.OK)
                            .body(documents.values().iterator().next());
                }

                String body = "[" + documentPaths.stream()
                        .map(key -> '"' + key + '"')
                        .collect(Collectors.joining(",")) + "]";

                return ResponseEntity.status(HttpStatus.OK).body(body);
            }
        };
    }

    /**
     * Read handler.
     *
     * @param name document name
     * @return response The Swagger JSON document
     */
    @GetMapping(value = "/{name}", produces = JSON_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> list(@RequestHeader Map<String, String> requestHeaders,
                                                 @PathVariable("name") String name) {

        final String apiVersion = Utils.getApiVersion(requestHeaders);
        final String encodedName = Encode.forHtml(name);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                Pair<String, String> lookupKey = Pair.of(apiVersion, encodedName);
                if (documents.containsKey(lookupKey)) {
                    return ResponseEntity.status(HttpStatus.OK).body(documents.get(lookupKey));
                }
                return ResponseEntity.status(404).body("Unknown document: " + encodedName);
            }
        };
    }
}
