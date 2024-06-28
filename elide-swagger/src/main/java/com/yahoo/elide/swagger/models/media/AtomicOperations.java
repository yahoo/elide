/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

/**
 * Atomic operations.
 */
public class AtomicOperations extends ObjectSchema {
    public AtomicOperations() {
        ArraySchema atomicOperations = new ArraySchema();
        atomicOperations.items(new AtomicOperation());
        this.addProperty("atomic:operations", atomicOperations);
    }
}
