/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.ARGS_KEY;

import com.yahoo.elide.core.request.Argument;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Context for resolving args under $$table. eg: {{$$table.args.arg1}}.
 */
@Getter
@ToString
public class TableSubContext extends TableContext {

    @Builder(builderMethodName = "tableSubContextBuilder")
    public TableSubContext(Map<String, Argument> tableArguments) {
        super(tableArguments);
    }

    @Override
    public Object get(Object key) {

        if (key.equals(ARGS_KEY)) {
            return this.getTableArguments();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
