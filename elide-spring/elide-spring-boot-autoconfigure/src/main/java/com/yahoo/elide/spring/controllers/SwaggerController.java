/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.contrib.swagger.SwaggerBuilder;

import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.models.Swagger;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
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
    protected Map<String, String> documents;
    private static final String JSON_CONTENT_TYPE = "application/json";

    /**
     * Constructs the resource.
     *
     * @param docs Map of path parameter name to swagger document.
     */
    @Autowired(required = false)
    public SwaggerController(Map<String, Swagger> docs) {
        log.debug("Started ~~");
        documents = new HashMap<>();

        docs.forEach((key, value) -> {
            documents.put(key, SwaggerBuilder.getDocument(value));
        });
    }

    @Autowired(required = false)
    public SwaggerController(Swagger doc) {
        log.debug("Started ~~");
        documents = new HashMap<>();
        documents.put("", SwaggerBuilder.getDocument(doc));
    }

    @GetMapping(value = {"/", ""}, produces = JSON_CONTENT_TYPE)
    public ResponseEntity<String> list() {

        if (documents.size() == 1) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(documents.values().stream().findFirst().get());
        }

        String body = "[" + documents.keySet().stream()
                .map(key -> '"' + key + '"')
                .collect(Collectors.joining(",")) + "]";

        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    /**
     * Read handler.
     *
     * @param name document name
     * @return response The Swagger JSON document
     */
    @GetMapping(value = "/{name}", produces = JSON_CONTENT_TYPE)
    public ResponseEntity<String> list(@PathVariable("name") String name) {
        String encodedName = Encode.forHtml(name);

        if (documents.containsKey(encodedName)) {
            return ResponseEntity.status(HttpStatus.OK).body(documents.get(encodedName));
        }
        return ResponseEntity.status(404).body("Unknown document: " + encodedName);
    }
}
