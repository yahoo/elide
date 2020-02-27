/*
 * Copyright 2019, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.security.User;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.security.AuthenticationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
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
public class JsonApiController {

    private final Elide elide;
    private final ElideConfigProperties settings;
    public static final String JSON_API_CONTENT_TYPE = JSONAPI_CONTENT_TYPE;

    @Autowired
    public JsonApiController(Elide elide, ElideConfigProperties settings) {
        log.debug("Started ~~");
        this.settings = settings;
        this.elide = elide;
    }

    @GetMapping(value = "/**", produces = JSON_API_CONTENT_TYPE)
    public ResponseEntity<String> elideGet(@RequestParam Map<String, String> allRequestParams,
                                           HttpServletRequest request, Authentication authentication) {
        String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());

        User user = new AuthenticationUser(authentication);
        ElideResponse response = elide.get(pathname, new MultivaluedHashMap<>(allRequestParams), user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }

    @PostMapping(value = "/**", consumes = JSON_API_CONTENT_TYPE, produces = JSON_API_CONTENT_TYPE)
    public ResponseEntity<String> elidePost(@RequestBody String body,
                                            HttpServletRequest request, Authentication authentication) {
        String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());

        User user = new AuthenticationUser(authentication);
        ElideResponse response = elide
                .post(pathname, body, user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }

    @PatchMapping(value = "/**", consumes = JSON_API_CONTENT_TYPE)
    public ResponseEntity<String> elidePatch(@RequestBody String body,
                                             HttpServletRequest request, Authentication authentication) {
        String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());

        User user = new AuthenticationUser(authentication);
        ElideResponse response = elide
                .patch(JSON_API_CONTENT_TYPE, JSON_API_CONTENT_TYPE, pathname, body, user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }

    @DeleteMapping(value = "/**")
    public ResponseEntity<String> elideDelete(HttpServletRequest request,
                                              Authentication authentication) {
        String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());

        User user = new AuthenticationUser(authentication);
        ElideResponse response = elide
                .delete(pathname, null, user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }

    @DeleteMapping(value = "/**", consumes = JSON_API_CONTENT_TYPE)
    public ResponseEntity<String> elideDeleteRelationship(@RequestBody String body,
                                                          HttpServletRequest request, Authentication authentication) {
        String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());

        User user = new AuthenticationUser(authentication);
        ElideResponse response = elide
                .delete(pathname, body, user);
        return ResponseEntity.status(response.getResponseCode()).body(response.getBody());
    }

    private String getJsonApiPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return pathname.replaceFirst(prefix, "");
    }
}
