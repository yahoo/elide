/*
 * Copyright 2019, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.security.AuthenticationUser;
import com.yahoo.elide.utils.HeaderUtils;
import org.apache.commons.lang3.StringUtils;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
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

    private final Elide elide;
    private final ElideConfigProperties settings;
    private final HeaderUtils.HeaderProcessor headerProcessor;
    private final RouteResolver routeResolver;

    public JsonApiController(RefreshableElide refreshableElide, ElideConfigProperties settings,
            RouteResolver routeResolver) {
        log.debug("Started ~~");
        this.settings = settings;
        this.elide = refreshableElide.getElide();
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.routeResolver = routeResolver;
    }

    private <K, V> MultivaluedHashMap<K, V> convert(MultiValueMap<K, V> springMVMap) {
        MultivaluedHashMap<K, V> convertedMap = new MultivaluedHashMap<>(springMVMap.size());
        springMVMap.forEach(convertedMap::put);
        return convertedMap;
    }

    @GetMapping(value = "/**", produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<String>> elideGet(@RequestHeader HttpHeaders requestHeaders,
                                                     @RequestParam MultiValueMap<String, String> allRequestParams,
                                                     HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.get(route.getBaseUrl(), route.getPath(),
                        convert(allRequestParams), requestHeadersCleaned,
                        user, route.getApiVersion(), UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @PostMapping(value = "/**", consumes = JsonApi.MEDIA_TYPE, produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<String>> elidePost(@RequestHeader HttpHeaders requestHeaders,
                                                      @RequestParam MultiValueMap<String, String> allRequestParams,
                                                      @RequestBody String body,
                                                      HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        String prefix = settings.getJsonApi().getPath();
        final String pathname = getPath(request, prefix);
        if (request.getRequestURI().startsWith(prefix + "/tableExport")) {
            prefix = "";
        }
        final String baseUrl = getBaseUrl(prefix);
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.post(route.getBaseUrl(), route.getPath(), body,
                        convert(allRequestParams), requestHeadersCleaned, user, route.getApiVersion(),
                        UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
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
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.patch(route.getBaseUrl(), request.getContentType(),
                        request.getContentType(), route.getPath(), body, convert(allRequestParams),
                        requestHeadersCleaned, user, route.getApiVersion(), UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
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
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide.delete(route.getBaseUrl(), route.getPath(), null,
                        convert(allRequestParams), requestHeadersCleaned,
                        user, route.getApiVersion(), UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
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
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide
                        .delete(route.getBaseUrl(), route.getPath(), body, convert(allRequestParams),
                                requestHeadersCleaned, user, route.getApiVersion(), UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
            }
        };
    }

    @PostMapping(
            value = "/operations",
            consumes = JsonApi.AtomicOperations.MEDIA_TYPE,
            produces = JsonApi.AtomicOperations.MEDIA_TYPE
    )
    public Callable<ResponseEntity<String>> elideOperations(@RequestHeader HttpHeaders requestHeaders,
                                                       @RequestParam MultiValueMap<String, String> allRequestParams,
                                                       @RequestBody String body,
                                                       HttpServletRequest request, Authentication authentication) {
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getJsonApi().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JSON_API_CONTENT_TYPE, baseUrl, route.getPath(), requestHeaders, allRequestParams);
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> call() throws Exception {
                ElideResponse response = elide
                        .operations(route.getBaseUrl(), request.getContentType(), request.getContentType(), pathname, body,
                               convert(allRequestParams), requestHeadersCleaned, user, route.getApiVersion(),
                               UUID.randomUUID());
                return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
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
