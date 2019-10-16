/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;

/**
 * Interface that constructs {@link QueryEngine} based on given entityDictionary.
 */
public interface QueryEngineFactory {

    public QueryEngine buildQueryEngine(EntityDictionary dictionary);

}
