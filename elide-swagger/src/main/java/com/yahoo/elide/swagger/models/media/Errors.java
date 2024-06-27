/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;

/**
 * Errors.
 */
public class Errors extends ArraySchema {
    /**
     * Used to construct errors.
     */
    public Errors() {
        this.items(new Error());
    }
}
