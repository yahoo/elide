/*
 * Copyright 2019, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.controllers;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.spring.security.AuthenticationUser;
import com.paiondata.elide.utils.HeaderProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Spring rest controller for Elide JSON-API.
 * Based on 'https://github.com/illyasviel/elide-spring-boot/'
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.json-api.path}")
public class JsonApiController {
    private final JsonApi jsonApi;
    private final Elide elide;
    private final ElideConfigProperties settings;
    private final HeaderProcessor headerProcessor;
    private final RouteResolver routeResolver;

    public JsonApiController(JsonApi jsonApi, ElideConfigProperties settings,
            RouteResolver routeResolver) {
        log.debug("Started ~~");
        this.jsonApi = jsonApi;
        this.settings = settings;
        this.elide = jsonApi.getElide();
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.routeResolver = routeResolver;
    }

    @GetMapping(value = "/**", produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<String>> elideGet(@RequestHeader HttpHeaders requestHeaders,
                                                     @RequestParam MultiValueMap<String, String> allRequestParams,
                                                     HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeadersCleaned,
                allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse<String> response = jsonApi.get(route, user, UUID.randomUUID());
                return ResponseEntity.status(response.getStatus()).body(response.getBody());
            }
        };
    }

    @PostMapping(value = "/**", consumes = { JsonApi.MEDIA_TYPE, JsonApi.AtomicOperations.MEDIA_TYPE }, produces = {
            JsonApi.MEDIA_TYPE, JsonApi.AtomicOperations.MEDIA_TYPE })
    public Callable<ResponseEntity<String>> elidePost(@RequestHeader HttpHeaders requestHeaders,
                                                      @RequestParam MultiValueMap<String, String> allRequestParams,
                                                      @RequestBody String body,
                                                      HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeadersCleaned,
                allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                if ("/operations".equals(route.getPath()) || "operations".equals(route.getPath())) {
                    // Atomic Operations
                    ElideResponse<String> response = jsonApi.operations(route, body, user, UUID.randomUUID());
                    return ResponseEntity.status(response.getStatus())
                            .contentType(MediaType.valueOf(JsonApi.AtomicOperations.MEDIA_TYPE))
                            .body(response.getBody());
                } else {
                    ElideResponse<String> response = jsonApi.post(route, body, user, UUID.randomUUID());
                    return ResponseEntity.status(response.getStatus())
                            .contentType(MediaType.valueOf(JsonApi.MEDIA_TYPE)).body(response.getBody());
                }
            }
        };
    }

    @PatchMapping(
            value = "/**",
            consumes = { JsonApi.MEDIA_TYPE, JsonApi.JsonPatch.MEDIA_TYPE },
            produces = JsonApi.MEDIA_TYPE
    )
    public Callable<ResponseEntity<String>> elidePatch(@RequestHeader HttpHeaders requestHeaders,
                                                       @RequestParam MultiValueMap<String, String> allRequestParams,
                                                       @RequestBody String body,
                                                       HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeadersCleaned,
                allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse<String> response = jsonApi.patch(route, body, user, UUID.randomUUID());
                return ResponseEntity.status(response.getStatus()).body(response.getBody());
            }
        };
    }

    @DeleteMapping(value = "/**", produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<String>> elideDelete(@RequestHeader HttpHeaders requestHeaders,
                                                        @RequestParam MultiValueMap<String, String> allRequestParams,
                                                        HttpServletRequest request,
                                                        Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeadersCleaned,
                allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse<String> response = jsonApi.delete(route, null, user, UUID.randomUUID());
                return ResponseEntity.status(response.getStatus()).body(response.getBody());
            }
        };
    }

    @DeleteMapping(value = "/**", consumes = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<String>> elideDeleteRelation(
            @RequestHeader HttpHeaders requestHeaders,
            @RequestParam MultiValueMap<String, String> allRequestParams,
            @RequestBody String body,
            HttpServletRequest request,
            Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeadersCleaned,
                allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse<String> response = jsonApi
                        .delete(route, body, user, UUID.randomUUID());
                return ResponseEntity.status(response.getStatus()).body(response.getBody());
            }
        };
    }

    private String getPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return pathname.replaceFirst(prefix, "");
    }

    protected String getBaseUrl(String prefix) {
        String baseUrl = elide.getElideSettings().getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        if (prefix.length() > 1) {
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1) + prefix;
            } else {
                baseUrl = baseUrl + prefix;
            }
        }

        return baseUrl;
    }
}
