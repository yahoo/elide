/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ChangeSpec for a particular field.
 */
@AllArgsConstructor
public class ChangeSpec {
    @Getter private final PersistentResource resource;
    @Getter private final String fieldName;
    @Getter private final Object original;
    @Getter private final Object modified;

    @Override
    public String toString() {
        return String.format("ChangeSpec { resource=%s, field=%s, original=%s, modified=%s}",
                safe(resource),
                fieldName,
                safe(original),
                safe(modified));
    }

    private String safe(Object object) {
        try {
            return String.valueOf(object);
        } catch (Exception e) {
            return e.toString();
        }
    }
}
