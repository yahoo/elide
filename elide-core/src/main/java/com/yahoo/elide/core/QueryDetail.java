/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * QueryDetail implementation class. Stores the model name (root entity class) and
 * the queryText that the underlying datastore will eventually run
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class QueryDetail {
    @Getter private String modelName;
    @Getter private String queryText;
    @Getter private Boolean isCached;
}
