/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.spring.config.ElideConfigProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

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

    private final QueryRunner runner;
    private final ElideConfigProperties settings;

    private static final String JSON_CONTENT_TYPE = "application/json";

    @Autowired
    public GraphqlController(Elide elide, ElideConfigProperties settings) {
        log.debug("Started ~~");
        this.runner = new QueryRunner(elide);
        this.settings = settings;
    }

    /**
     * Single entry point for GraphQL requests.
     *
     * @param graphQLDocument post data as json document
     * @param user The user principal
     * @return response
     */
    @PostMapping(value = {"/**", ""}, consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public ResponseEntity<String> post(@RequestBody String graphQLDocument, Principal user) {

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + settings.getGraphql().getPath() + "/";

        ElideResponse response = runner.run(baseUrl, graphQLDocument, user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }
}
