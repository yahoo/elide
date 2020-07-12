/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

/**
 * QueryDetail implementation class. Stores the model name (root entity class) and
 * the queryText that the underlying datastore will eventually run
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class QueryDetail {
    private String modelName;
    private List<String> queryText;
}
