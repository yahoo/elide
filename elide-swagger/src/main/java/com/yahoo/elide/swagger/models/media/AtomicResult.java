/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Atomic result.
 */
public class AtomicResult extends ObjectSchema {
    public AtomicResult() {
        ObjectSchema data = new ObjectSchema();
        data.addProperty("link", new ObjectSchema());
        data.addProperty("type", new StringSchema());
        data.addProperty("id", new StringSchema());
        data.addProperty("attributes", new ObjectSchema());

        this.addProperty("data", data);
    }
}
