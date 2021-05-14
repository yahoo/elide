/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.ARGS_KEY;

import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Context for resolving args under $$table. eg: {{$$table.args.arg1}}.
 */
@Getter
@ToString
public class TableSubContext extends TableContext {

    @Builder(builderMethodName = "tableSubContextBuilder")
    public TableSubContext(Queryable queryable) {
        super(queryable);
    }

    @Override
    public Object get(Object key) {

        if (key.equals(ARGS_KEY)) {
            return this.queryable.getAvailableArguments();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
