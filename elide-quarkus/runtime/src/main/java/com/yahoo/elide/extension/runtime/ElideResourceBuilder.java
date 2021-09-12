package com.yahoo.elide.extension.runtime;

import com.yahoo.elide.graphql.GraphQLEndpoint;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.swagger.resources.DocEndpoint;
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
    public static final String SWAGGER_BASE = "/doc";

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
        } else if (clazz.equals(DocEndpoint.class)) {
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
