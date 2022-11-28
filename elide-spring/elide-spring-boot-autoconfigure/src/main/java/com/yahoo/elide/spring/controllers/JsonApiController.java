/*
 * Copyright 2019, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.security.AuthenticationUser;
import com.yahoo.elide.utils.HeaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Spring rest controller for Elide JSON-API.
 * Based on 'https://github.com/illyasviel/elide-spring-boot/'
 */
@Slf4j
@RestController
@Configuration
@RequestMapping(value = "${elide.json-api.path}")
@ConditionalOnExpression("${elide.json-api.enabled:false}")
@RefreshScope
public class JsonApiController {

    private final Elide elide;
    private final ElideConfigProperties settings;
    private final HeaderUtils.HeaderProcessor headerProcessor;
    public static final String JSON_API_CONTENT_TYPE = JSONAPI_CONTENT_TYPE;
    public static final String JSON_API_PATCH_CONTENT_TYPE = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;

    @Autowired
    public JsonApiController(RefreshableElide refreshableElide, ElideConfigProperties settings) {
        log.debug("Started ~~");
        this.settings = settings;
        this.elide = refreshableElide.getElide();
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
    }

    private <K, V> MultivaluedHashMap<K, V> convert(MultiValueMap<K, V> springMVMap) {
        MultivaluedHashMap<K, V> convertedMap = new MultivaluedHashMap<>(springMVMap.size());
        springMVMap.forEach(convertedMap::put);
        return convertedMap;
    }

    @GetMapping(value = "/**", produces = JSON_API_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> elideGet(@RequestHeader HttpHeaders requestHeaders,
                                                     @RequestParam MultiValueMap<String, String> allRequestParams,
                                                     HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.get(baseUrl, pathname,
                        convert(allRequestParams), requestHeadersCleaned,
                        user, apiVersion, UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @PostMapping(value = "/**", consumes = JSON_API_CONTENT_TYPE, produces = JSON_API_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> elidePost(@RequestHeader HttpHeaders requestHeaders,
                                                      @RequestParam MultiValueMap<String, String> allRequestParams,
                                                      @RequestBody String body,
                                                      HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.post(baseUrl, pathname, body, convert(allRequestParams),
                        requestHeadersCleaned, user, apiVersion, UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @PatchMapping(
            value = "/**",
            consumes = { JSON_API_CONTENT_TYPE, JSON_API_PATCH_CONTENT_TYPE},
            produces = JSON_API_CONTENT_TYPE
    )
    public Callable<ResponseEntity<String>> elidePatch(@RequestHeader HttpHeaders requestHeaders,
                                                       @RequestParam MultiValueMap<String, String> allRequestParams,
                                                       @RequestBody String body,
                                                       HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide
                        .patch(baseUrl, request.getContentType(), request.getContentType(), pathname, body,
                               convert(allRequestParams), requestHeadersCleaned, user, apiVersion,
                               UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @DeleteMapping(value = "/**")
    public Callable<ResponseEntity<String>> elideDelete(@RequestHeader HttpHeaders requestHeaders,
                                                        @RequestParam MultiValueMap<String, String> allRequestParams,
                                                        HttpServletRequest request,
                                                        Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.delete(baseUrl, pathname, null,
                        convert(allRequestParams), requestHeadersCleaned,
                        user, apiVersion, UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @DeleteMapping(value = "/**", consumes = JSON_API_CONTENT_TYPE)
    public Callable<ResponseEntity<String>> elideDeleteRelation(
            @RequestHeader HttpHeaders requestHeaders,
            @RequestParam MultiValueMap<String, String> allRequestParams,
            @RequestBody String body,
            HttpServletRequest request,
            Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide
                        .delete(baseUrl, pathname, body, convert(allRequestParams),
                                requestHeadersCleaned, user, apiVersion, UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    private String getJsonApiPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return pathname.replaceFirst(prefix, "");
    }

    protected String getBaseUrlEndpoint() {
        String baseUrl = elide.getElideSettings().getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        return baseUrl;
    }
}
