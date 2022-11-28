/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.type.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
class Operation {
    enum OpType {
        CREATE,
        DELETE,
        UPDATE
    };

    @Getter private final String id;
    @Getter private final Object instance;
    @Getter private final Type<?> type;
    @Getter private final OpType opType;
}
