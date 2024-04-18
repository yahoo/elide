/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.runtime;

import com.paiondata.elide.graphql.GraphQLEndpoint;
import com.paiondata.elide.jsonapi.resources.JsonApiEndpoint;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;
//import com.paiondata.elide.swagger.resources.DocEndpoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;

/**
 * Overrides JAX-RS default behavior of extracting a resource path from the Path annotation for Elide endpoints.
 * Instead, the resource paths are mapped to application configuration.
 */
public class ElideResourceBuilder extends ResourceBuilder {

    private static final String BASE_PATH = "/";

    public static final String JSONAPI_BASE = "/jsonapi";
    public static final String GRAPHQL_BASE = "/graphql";
    public static final String SWAGGER_BASE = "/apiDocs";

    @Override
    protected ResourceClassBuilder createResourceClassBuilder(Class<?> clazz) {
        Config config = ConfigProvider.getConfig();

        String basePath = null;

        if (clazz.equals(JsonApiEndpoint.class)) {
            basePath = config.getConfigValue("elide.base-jsonapi").getValue();
            if (basePath == null) {
                basePath = JSONAPI_BASE;
            }
        } else if (clazz.equals(GraphQLEndpoint.class)) {
            basePath = config.getConfigValue("elide.base-graphql").getValue();
            if (basePath == null) {
                basePath = GRAPHQL_BASE;
            }
        } else if (clazz.equals(ApiDocsEndpoint.class)) {
            basePath = config.getConfigValue("elide.base-swagger").getValue();
            if (basePath == null) {
                basePath = SWAGGER_BASE;
            }
        } else {
            super.createResourceClassBuilder(clazz);
        }

        if (! basePath.startsWith(BASE_PATH)) {
            basePath = BASE_PATH + basePath;
        }

        return buildRootResource(clazz, basePath);
    }
}
