/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
class Operation {
    @Getter private final String id;
    @Getter private final Object instance;
    @Getter private final Class<?> type;
    @Getter private final Boolean delete;
}
