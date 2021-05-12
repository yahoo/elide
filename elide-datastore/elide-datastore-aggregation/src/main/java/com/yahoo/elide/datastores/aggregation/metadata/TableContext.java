/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Context for resolving table arguments in provided expression.
 */
@Getter
@ToString
public class TableContext extends ColumnContext {

    @Builder(builderMethodName = "tableContextBuilder")
    public TableContext(Queryable queryable) {
        super(null, queryable, null, null);
    }

    public Object get(Object key) {

        if (key.equals(TBL_PREFIX)) {
            return getTableArgMap(this.queryable);
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
