/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ChangeSpec for a particular field.
 *
 * @param <T> type parameter
 */
@AllArgsConstructor
public class ChangeSpec<T> {
    @Getter private final String parentType;
    @Getter private final String fieldName;
    @Getter private final T added;
    @Getter private final T removed;
}
