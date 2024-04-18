/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;

/**
 * Properties to configure the API Versioning Strategy.
 */
@Data
public class ApiVersioningStrategyProperties {
    /**
     * Configure the path based versioning strategy.
     */
    @Data
    public static class Path {
        /**
         * Whether or not the path based versioning strategy is enabled.
         */
        private boolean enabled = true;

        /**
         * The version prefix to use. For instance /v1/resource.
         */
        private String versionPrefix = "v";
    }

    /**
     * Configure the header based versioning strategy.
     */
    @Data
    public static class Header {
        /**
         * Whether or not the header based versioning strategy is enabled.
         */
        private boolean enabled = false;

        /**
         * The header names that contains the API version. For instance Accept-Version or ApiVersion.
         */
        private String[] headerName = new String[] { "Accept-Version" };
    }

    /**
     * Configure the parameter based versioning strategy.
     */
    @Data
    public static class Parameter {
        /**
         * Whether or not the parameter based versioning strategy is enabled.
         */
        private boolean enabled = false;

        /**
         * The parameter name that contains the API version.
         */
        private String parameterName = "v";
    }

    /**
     * Configure the media type profile strategy.
     * <p>
     * For instance {@code Accept: application/vnd.api+json; profile=https://elide.io/api/v1}
     */
    @Data
    public static class MediaTypeProfile {
        /**
         * Whether or not the media type profile versioning strategy is enabled.
         */
        private boolean enabled = false;

        /**
         * The version prefix to use for the version.
         */
        private String versionPrefix = "v";

        /**
         * The uri prefix to use to determine the profile that contains the API version.
         */
        private String uriPrefix = "";
    }


    /**
     * Configure the path based versioning strategy.
     */
    private Path path = new Path();

    /**
     * Configure the header based versioning strategy.
     */
    private Header header = new Header();

    /**
     * Configure the parameter based versioning strategy.
     */
    private Parameter parameter = new Parameter();

    /**
     * Configure the media type profile strategy.
     * <p>
     * For instance {@code Accept: application/vnd.api+json; profile=https://elide.io/api/v1}
     */
    private MediaTypeProfile mediaTypeProfile = new MediaTypeProfile();
}
