package com.yahoo.elide.extension.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "elide", phase = ConfigPhase.BUILD_TIME)
public class ElideConfig {

    /**
     * Default page size if client doesn't request any.
     */
    @ConfigItem(defaultValue = "100")
    public int defaultPageSize;

    /**
     * Maximum page size that can be requested by a client.
     */
    @ConfigItem(defaultValue = "10000")
    public int defaultMaxPageSize;

    /**
     * Turns on verbose errors in HTTP responses.
     */
    @ConfigItem(defaultValue = "true")
    public boolean verboseErrors;

    /**
     * The base URL path prefix for Elide service endpoints.
     */
    @ConfigItem(defaultValue = "/")
    public String basePath;

    /**
     * The base URL path prefix for Elide JSON-API service endpoints.
     * This is appended to the basePath.
     */
    @ConfigItem
    public String baseJsonapi;

    /**
     * The base URL path prefix for Elide GraphQL service endpoints.
     * This is appended to the basePath.
     */
    @ConfigItem
    public String baseGraphql;

    /**
     * The base URL path prefix for the Elide Swagger document.
     * This is appended to the basePath.
     */
    @ConfigItem
    public String baseSwagger;
}
