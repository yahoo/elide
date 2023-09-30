/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Properties to configure the API Versioning Strategy.
 */
@Data
public class ApiVersioningStrategyProperties {
    @Data
    public static class Path {
        private boolean enabled = true;
        private String versionPrefix = "v";
    }

    @Data
    public static class Header {
        private boolean enabled = false;
        private String[] headerName = new String[] { "Accept-Version" };
    }

    @Data
    public static class Parameter {
        private boolean enabled = false;
        private String parameterName = "v";
    }

    @Data
    public static class MediaTypeProfile {
        private boolean enabled = false;
        private String versionPrefix = "v";
        private String uriPrefix = "";
    }

    private Path path = new Path();
    private Header header = new Header();
    private Parameter parameter = new Parameter();
    private MediaTypeProfile mediaTypeProfile = new MediaTypeProfile();
}
