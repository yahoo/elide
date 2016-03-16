/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.PersistentResource;
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
}
