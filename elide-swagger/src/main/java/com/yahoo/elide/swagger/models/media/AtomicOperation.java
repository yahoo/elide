/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Atomic operation.
 */
public class AtomicOperation extends ObjectSchema {
    public AtomicOperation() {
        this.addProperty("op", new StringSchema()._enum(List.of("add", "update", "remove")));
        this.addProperty("ref",
                new ObjectSchema().addProperty("type", new StringSchema()).addProperty("id", new StringSchema())
                        .addProperty("lid", new StringSchema()).addProperty("relationship", new StringSchema()));
        this.addProperty("href", new StringSchema());
        Schema<?> data = new ObjectSchema().addProperty("type", new StringSchema())
                .addProperty("id", new StringSchema()).addProperty("lid", new StringSchema())
                .addProperty("attributes", new ObjectSchema());
        data = data.nullable(true); // For OpenAPI 3.0
        data.getTypes().add("null"); // For OpenAPI 3.1
        @SuppressWarnings("rawtypes")
        List<Schema> anyOfData = new ArrayList<>();
        anyOfData.add(new ArraySchema().items(data));
        anyOfData.add(data);
        this.addProperty("data", new Schema<>().anyOf(anyOfData));
        this.addProperty("meta", new ObjectSchema());
        this.required(List.of("op"));

        Schema<?> refOrHref = new Schema<>().not(new ObjectSchema().required(List.of("ref", "href")));
        this.allOf(List.of(refOrHref));
    }
}
