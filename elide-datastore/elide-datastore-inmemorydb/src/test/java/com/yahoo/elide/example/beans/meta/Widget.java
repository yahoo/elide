/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.example.beans.meta;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;

import jakarta.persistence.Id;

import java.util.Optional;
import java.util.Set;

@Include
public class Widget implements WithMetadata {
    @Id
    private String id;

    @Override
    public void setMetadataField(String property, Object value) {
        //NOOP
    }

    @Override
    public Optional<Object> getMetadataField(String property) {
        if (property.equals("foo")) {
            return Optional.of("bar");
        }

        return Optional.empty();
    }

    @Override
    public Set<String> getMetadataFields() {
        return Set.of("foo");
    }
}
