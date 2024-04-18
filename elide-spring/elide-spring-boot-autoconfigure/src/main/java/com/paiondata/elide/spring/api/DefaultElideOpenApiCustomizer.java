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
        JsonApiSettings jsonApiSettings = this.elide.getElide().getElideSettings().getSettings(JsonApiSettings.class);

        for (String apiVersion : this.elide.getElide().getElideSettings().getEntityDictionary().getApiVersions()) {
            OpenApiBuilder builder = new OpenApiBuilder(this.elide.getElide().getElideSettings().getEntityDictionary())
                    .apiVersion(apiVersion).basePath(jsonApiSettings.getPath());
            if (this.settings.getApiVersioningStrategy().getPath().isEnabled()) {
                if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                    String path = jsonApiSettings.getPath();
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }
                    path = path + this.settings.getApiVersioningStrategy().getPath().getVersionPrefix() + apiVersion;
                    builder.basePath(path);
                }
            } else if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
                // Regardless of the api versioning strategy the NO_VERSION one needs to be
                // applied so other versions shall be skipped
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
