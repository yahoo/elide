/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.swagger.OpenApiBuilder;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Default implementation for Elide OpenApiCustomizer to apply to the
 * GroupedOpenApi to contribute to SpringDoc.
 */
public class DefaultElideGroupedOpenApiCustomizer implements ElideGroupedOpenApiCustomizer {

    private final RefreshableElide elide;
    private final ElideConfigProperties settings;
    private final PathMatcher pathMatcher;

    public DefaultElideGroupedOpenApiCustomizer(RefreshableElide elide, ElideConfigProperties settings,
            PathMatcher pathMatcher) {
        this.elide = elide;
        this.settings = settings;
        this.pathMatcher = pathMatcher;
    }

    public DefaultElideGroupedOpenApiCustomizer(RefreshableElide elide, ElideConfigProperties settings) {
        this(elide, settings, new AntPathMatcher());
    }

    @Override
    public void customize(GroupedOpenApi groupedOpenApi) {
        groupedOpenApi.getOpenApiCustomizers().add(0, new ElideOpenApiCustomizer() {
            @Override
            public void customise(OpenAPI openApi) {
                OpenApis.removePathsByTags(openApi, "graphql-controller", "api-docs-controller", "json-api-controller");
            }
        });
        for (String apiVersion : this.elide.getElide().getElideSettings().getEntityDictionary().getApiVersions()) {
            JsonApiSettings jsonApiSettings = this.elide.getElide().getElideSettings()
                    .getSettings(JsonApiSettings.class);
            String path = jsonApiSettings.getPath();
            if (this.settings.getApiVersioningStrategy().getPath().isEnabled()) {
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }
                    path = path + this.settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion;
                }
            } else if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                // Regardless of the api versioning strategy the NO_VERSION one needs to be
                // applied so other versions shall be skipped
                continue;
            }

            if (match(groupedOpenApi, path)) {
                String basePath = path;
                groupedOpenApi.getOpenApiCustomizers().add(0, new ElideOpenApiCustomizer() {
                    @Override
                    public void customise(OpenAPI openApi) {
                        OpenApiBuilder builder = new OpenApiBuilder(
                                elide.getElide().getElideSettings().getEntityDictionary()).apiVersion(apiVersion)
                                .basePath(basePath);
                        builder.applyTo(openApi);
                    }
                });
            }
        }
    }

    protected boolean match(GroupedOpenApi groupedOpenApi, String path) {
        // Check if the path matches
        boolean matches = false;
        if (groupedOpenApi.getPathsToMatch() != null) {
            for (String pathToMatch : groupedOpenApi.getPathsToMatch()) {
                if (pathMatcher.match(pathToMatch, path)) {
                    matches = true;
                    break;
                }
            }
        }
        if (matches && groupedOpenApi.getPathsToExclude() != null) {
            for (String pathToExclude : groupedOpenApi.getPathsToExclude()) {
                if (pathMatcher.match(pathToExclude, path)) {
                    matches = false;
                    break;
                }
            }
        }
        return matches;
    }
}
