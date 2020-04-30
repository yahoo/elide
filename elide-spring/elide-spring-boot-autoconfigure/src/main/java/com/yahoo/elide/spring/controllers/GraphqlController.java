/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.graphql.QueryRunner.buildErrorResponse;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;
import com.yahoo.elide.spring.config.ElideConfigProperties;

import com.yahoo.elide.spring.security.AuthenticationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Spring rest controller for Elide GraphQL.
 */
@Slf4j
@Configuration
@RestController
@RequestMapping(value = "${elide.graphql.path}")
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.graphql.enabled:false}")
public class GraphqlController {

    private final Map<String, QueryRunner> runners;
    private final Elide elide;

    private static final String JSON_CONTENT_TYPE = "application/json";

    @Autowired
    public GraphqlController(Elide elide) {
        log.debug("Started ~~");
        this.elide = elide;
        this.runners = new HashMap<>();
        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }
    }

    /**
     * Single entry point for GraphQL requests.
     *
     * @param graphQLDocument post data as json document
     * @param principal The user principal
     * @return response
     */
    @PostMapping(value = {"/**", ""}, consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> post(@RequestHeader Map<String, String> requestHeaders,
                                                 @RequestBody String graphQLDocument, Authentication principal) {
        final User user = new AuthenticationUser(principal);
        final String apiVersion = Utils.getApiVersion(requestHeaders);
        final QueryRunner runner = runners.get(apiVersion);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response;
                if (runner == null) {
                    response = buildErrorResponse(elide, new InvalidOperationException("Invalid API Version"), false);
                } else {
                    response = runner.run(graphQLDocument, user);
                }

                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }
}
