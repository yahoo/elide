/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.ARGS_KEY;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Context for resolving table arguments in provided expression.
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

            if (queryable instanceof SQLTable) {
                SQLTable table = (SQLTable) queryable;
                return getDefaultArgumentsMap(table.getArguments());
            }

            Query query = (Query) queryable;
            return query.getArguments();
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    public static Map<String, Argument> getDefaultArgumentsMap(
                    Set<com.yahoo.elide.datastores.aggregation.metadata.models.Argument> availableArgs) {

         return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .map(arg -> Argument.builder()
                                        .name(arg.getName())
                                        .value(arg.getDefaultValue())
                                        .build())
                        .collect(Collectors.toMap(Argument::getName, Function.identity()));
    }
}
