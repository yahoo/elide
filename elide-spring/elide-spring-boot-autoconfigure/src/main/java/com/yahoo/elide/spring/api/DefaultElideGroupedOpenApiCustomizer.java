/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.api;

import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.swagger.OpenApiBuilder;

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
        for (String apiVersion : this.elide.getElide().getElideSettings().getDictionary().getApiVersions()) {
            String path = this.elide.getElide().getElideSettings().getJsonApiPath();
            if (EntityDictionary.NO_VERSION.equals(apiVersion)
                    || this.settings.getApiVersioningStrategy().getPath().isEnabled()) {

                if (!path.endsWith("/")) {
                    path = path + "/";
                }

                if (this.settings.getApiVersioningStrategy().getPath().isEnabled()) {
                    path = path + this.settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion;
                }

                if (match(groupedOpenApi, path)) {
                    String basePath = path;
                    groupedOpenApi.getOpenApiCustomizers().add(0, new ElideOpenApiCustomizer() {
                        @Override
                        public void customise(OpenAPI openApi) {
                            OpenApiBuilder builder = new OpenApiBuilder(
                                    elide.getElide().getElideSettings().getDictionary())
                                    .apiVersion(apiVersion).basePath(basePath);
                            builder.applyTo(openApi);
                        }
                    });
                }

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
