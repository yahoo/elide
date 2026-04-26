/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

/**
 * Atomic results.
 */
public class AtomicResults extends ObjectSchema {
    public AtomicResults() {
        ArraySchema atomicResults = new ArraySchema();
        atomicResults.items(new AtomicResult());
        this.addProperty("atomic:results", atomicResults);
    }
}
