/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;


import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
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
public class ColumnSubContext extends ColumnContext {

    @Builder(builderMethodName = "columnSubContextBuilder")
    public ColumnSubContext(MetaDataStore metaDataStore, Queryable queryable, String alias,
                    ColumnProjection column) {
        super(metaDataStore, queryable, alias, column);
    }

    @Override
    public Object get(Object key) {

        if (key.equals(ARGS_KEY)) {
            return this.getColumn().getArguments();
        }

        if (key.equals(EXPR_KEY)) {
            return ColumnContext.builder()
                            .queryable(this.getQueryable())
                            .alias(this.getAlias())
                            .metaDataStore(this.getMetaDataStore())
                            .column(this.getColumn())
                            .build()
                            .resolve(this.getColumn().getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
