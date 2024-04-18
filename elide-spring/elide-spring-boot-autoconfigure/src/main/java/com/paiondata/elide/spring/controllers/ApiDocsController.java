/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.controllers;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.swagger.OpenApiDocument;
import com.paiondata.elide.swagger.OpenApiDocument.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    protected RouteResolver routeResolver;

    protected ElideConfigProperties elideConfigProperties;

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
    public ApiDocsController(ApiDocsRegistrations docs, RouteResolver routeResolver,
            ElideConfigProperties elideConfigProperties) {
        log.debug("Started ~~");
        this.documents = new HashMap<>();
        this.routeResolver = routeResolver;
        this.elideConfigProperties = elideConfigProperties;

        docs.getRegistrations().forEach(doc -> {
            String apiVersion = doc.getApiVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;

            this.documents.put(Pair.of(apiVersion, apiPath),
                    new OpenApiDocument(doc.document, OpenApiDocument.Version.from(doc.version)));
        });
    }

    @GetMapping(value = { "/**", "" }, produces = MediaType.APPLICATION_JSON)
    public Callable<ResponseEntity<String>> listJson(@RequestHeader HttpHeaders requestHeaders,
            @RequestParam MultiValueMap<String, String> allRequestParams, HttpServletRequest request) {
        String prefix = elideConfigProperties.getApiDocs().getPath();
        String pathname = getPath(request, prefix);
        final String baseUrl = getBaseUrl(prefix);
        Route route = routeResolver.resolve(MediaType.APPLICATION_JSON, baseUrl, pathname, requestHeaders,
                allRequestParams);
        String path = route.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isBlank()) {
            return list(route.getApiVersion(), MediaType.APPLICATION_JSON);
        } else {
            return list(route.getApiVersion(), path, MediaType.APPLICATION_JSON);
        }
    }

    @GetMapping(value = { "/**", "" }, produces = MediaType.APPLICATION_YAML)
    public Callable<ResponseEntity<String>> listYaml(@RequestHeader HttpHeaders requestHeaders,
            @RequestParam MultiValueMap<String, String> allRequestParams, HttpServletRequest request) {
        String prefix = elideConfigProperties.getApiDocs().getPath();
        String pathname = getPath(request, prefix);
        final String baseUrl = getBaseUrl(prefix);
        Route route = routeResolver.resolve(MediaType.APPLICATION_YAML, baseUrl, pathname, requestHeaders,
                allRequestParams);
        String path = route.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isBlank()) {
            return list(route.getApiVersion(), MediaType.APPLICATION_YAML);
        } else {
            return list(route.getApiVersion(), path, MediaType.APPLICATION_YAML);
        }
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

    private String getPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        pathname = pathname.replaceFirst(prefix, "");
        try {
            return URLDecoder.decode(pathname, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return pathname;
        }
    }

    /**
     * Determines the base url for the api docs controller. eg. http://www.example.org/api-docs.
     *
     * @param prefix the api docs path eg. /api-docs
     * @return the base url for api docs
     */
    protected String getBaseUrl(String prefix) {
        // The base url of the application eg. http://www.example.org
        String baseUrl = elideConfigProperties.getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            // If not specified get from the current context path
            // Ie. including server.servlet.context-path
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        if (prefix.length() > 1) {
            if (baseUrl.endsWith("/")) {
                // Remove trailing / from application base url before appending
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1) + prefix;
            } else {
                baseUrl = baseUrl + prefix;
            }
        }

        return baseUrl;
    }
}
