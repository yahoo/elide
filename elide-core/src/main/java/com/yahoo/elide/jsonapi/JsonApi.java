/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

/**
 * JSON:API.
 */
public class JsonApi {
    private JsonApi() {
    }
    public static final String MEDIA_TYPE = "application/vnd.api+json";

    public static class Extensions {
        private Extensions() {
        }
        public static class JsonPatch {
            private JsonPatch() {
            }
            public static final String MEDIA_TYPE = "application/vnd.api+json; ext=jsonpatch";
        }

        public static class AtomicOperations {
            private AtomicOperations() {
            }
            public static final String MEDIA_TYPE = "application/vnd.api+json; ext=\"https://jsonapi.org/ext/atomic\"";
        }
    }
}
