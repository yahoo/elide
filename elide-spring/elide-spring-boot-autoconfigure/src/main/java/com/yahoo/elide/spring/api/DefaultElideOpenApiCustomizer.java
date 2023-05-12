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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation for Elide OpenApiCustomizer to contribute to SpringDoc.
 */
public class DefaultElideOpenApiCustomizer implements ElideOpenApiCustomizer {

    private final RefreshableElide elide;
    private final ElideConfigProperties settings;

    public DefaultElideOpenApiCustomizer(RefreshableElide elide, ElideConfigProperties settings) {
        this.elide = elide;
        this.settings = settings;
    }

    @Override
    public void customise(OpenAPI openApi) {
        removePaths(openApi);
        for (String apiVersion : this.elide.getElide().getElideSettings().getDictionary().getApiVersions()) {
            OpenApiBuilder builder = new OpenApiBuilder(this.elide.getElide().getElideSettings().getDictionary())
                    .apiVersion(apiVersion).basePath(this.elide.getElide().getElideSettings().getJsonApiPath());
            if (this.settings.getApiVersioningStrategy().getPath().isEnabled()) {
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    builder.basePath(this.elide.getElide().getElideSettings().getJsonApiPath() + "/"
                            + this.settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion);
                }
            } else if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                continue;
            }
            builder.applyTo(openApi);
        }
    }

    /**
     * Removes the unnecessary Elide controllers that were scanned by SpringDoc.
     *
     * @param openApi the document to remove paths from
     */
    protected void removePaths(OpenAPI openApi) {
        removePathsByTags(openApi, "graphql-controller", "api-docs-controller", "json-api-controller");
    }

    public static void removePathsByTags(OpenAPI openApi, String... tagsToRemove) {
        Set<String> tags = new HashSet<>();
        Collections.addAll(tags, tagsToRemove);
        removePathsByTags(openApi, tags);
    }

    public static void removePathsByTags(OpenAPI openApi, Set<String> tagsToRemove) {
        Set<String> pathsToRemove = new HashSet<>();
        Paths paths = openApi.getPaths();
        if (paths != null) {
            openApi.getPaths().forEach((path, pathItem) -> {
                Set<String> tags = new HashSet<>();
                if (pathItem.getGet() != null) {
                    tags.addAll(pathItem.getGet().getTags());
                } else if (pathItem.getPost() != null) {
                    tags.addAll(pathItem.getPost().getTags());
                }
                for (String tagToRemove : tagsToRemove) {
                    if (tags.contains(tagToRemove)) {
                        pathsToRemove.add(path);
                        break;
                    }
                }
            });
        }
        pathsToRemove.forEach(key -> openApi.getPaths().remove(key));
    }
}
