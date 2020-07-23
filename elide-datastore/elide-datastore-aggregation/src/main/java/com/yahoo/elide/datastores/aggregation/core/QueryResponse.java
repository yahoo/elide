/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * QueryLogger Response Class
 */
@AllArgsConstructor
public class QueryResponse {
    @Getter private final int responseCode;
    @Getter private final Iterable<Object> data;
    @Getter private final String errorMessage;
}
