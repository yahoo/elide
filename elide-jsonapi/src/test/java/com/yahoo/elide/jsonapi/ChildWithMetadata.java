/*
 * Copyright 2025, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test entity for verifying that per-resource {@link WithMetadata} fields are surfaced in the JSON-API response.
 */
@Entity
@Include(name = "childWithMetadata")
public class ChildWithMetadata implements WithMetadata {

    private long id;
    private final Map<String, Object> metadata = new HashMap<>();

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Transient
    @Override
    public void setMetadataField(String property, Object value) {
        metadata.put(property, value);
    }

    @Transient
    @Override
    public Optional<Object> getMetadataField(String property) {
        return Optional.ofNullable(metadata.get(property));
    }

    @Transient
    @Override
    public Set<String> getMetadataFields() {
        return metadata.keySet();
    }
}
