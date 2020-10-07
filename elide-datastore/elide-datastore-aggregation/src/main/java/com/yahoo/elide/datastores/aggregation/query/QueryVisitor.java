/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

public interface QueryVisitor<T> {
    public T visitQuery(Query query);
    public T visitTable(Table table);
}
