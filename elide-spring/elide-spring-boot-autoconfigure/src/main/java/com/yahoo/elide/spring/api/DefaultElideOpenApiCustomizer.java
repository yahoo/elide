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
                    String path = this.elide.getElide().getElideSettings().getJsonApiPath();
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }
                    path = path + this.settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion;
                    builder.basePath(path);
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
        OpenApis.removePathsByTags(openApi, "graphql-controller", "api-docs-controller", "json-api-controller");
    }
}
