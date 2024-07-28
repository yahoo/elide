/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Error.
 */
public class Error extends ObjectSchema {
    /**
     * Links.
     */
    public static class Links extends ObjectSchema {
        /**
         * Used to construct links.
         */
        public Links() {
            this.addProperty("about", new StringSchema());
            this.addProperty("type", new StringSchema());
        }
    }

    /**
     * Source.
     */
    public static class Source extends ObjectSchema {
        /**
         * Used to construct a source.
         */
        public Source() {
            this.addProperty("pointer", new StringSchema());
            this.addProperty("parameter", new StringSchema());
            this.addProperty("header", new StringSchema());
        }
    }

    /**
     * Used to construct an error.
     */
    public Error() {
        this.addProperty("id", new StringSchema());
        this.addProperty("links", new Links());
        this.addProperty("status", new StringSchema());
        this.addProperty("code", new StringSchema());
        this.addProperty("title", new StringSchema());
        this.addProperty("detail", new StringSchema());
        this.addProperty("source", new Source());
    }
}
