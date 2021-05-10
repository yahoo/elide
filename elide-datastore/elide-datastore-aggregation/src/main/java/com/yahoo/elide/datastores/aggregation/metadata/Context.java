/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Context for resolving handlebars.
 */
@Getter
@ToString
public abstract class Context extends HashMap<String, Object> {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";
    public static final String HANDLEBAR_PREFIX = "{{";
    public static final String HANDLEBAR_SUFFIX = "}}";

    protected final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .with((Formatter) (value, next) -> {
                if (value instanceof com.yahoo.elide.core.request.Argument) {
                    return ((com.yahoo.elide.core.request.Argument) value).getValue();
                }
                return next.format(value);
            })
            .registerHelper("sql", this::resolveSQLHandlebar);

    public Object get(Object key) {
        return get(key, emptyMap());
    }

    protected abstract Object get(Object key, Map<String, Argument> fixedArgs);

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     */
    public String resolve(String expression) {
        return resolveHandlebars(this, expression);
    }

    protected Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String column = options.hash("column");
        int argsIndex = column.indexOf('[');
        String invokedColumnName = column;

        Context currentCtx = (Context) context;
        // 'from' is optional, so if not provided use the same table context.
        Context invokedCtx = isBlank(from) ? currentCtx
                                           : (Context) currentCtx.get(from);

        if (argsIndex >= 0) {
            Map<String, Argument> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }
        return invokedCtx.get(invokedColumnName);
    }

    protected String resolveHandlebars(Context context, String expression) {

        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(context);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
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

    protected static Map<String, Object> getTableArgMap(Queryable queryable) {
        Map<String, Object> tblArgsMap = new HashMap<>();

        if (queryable instanceof SQLTable) {
            SQLTable table = (SQLTable) queryable;
            tblArgsMap.put(ARGS_KEY, getDefaultArgumentsMap(table.getArguments()));
            return tblArgsMap;
        }

        Query query = (Query) queryable;
        tblArgsMap.put(ARGS_KEY, query.getArguments());
        return tblArgsMap;
    }

    public static Map<String, Argument> getColumnArgMap(Queryable queryable,
                                                        String columnName,
                                                        Map<String, Argument> callingColumnArgs,
                                                        Map<String, Argument> fixedArgs) {

        Map<String, Argument> columnArgMap = new HashMap<>();

        queryable.getSource()
                 .getColumnProjection(columnName)
                 .getArguments()
                        .forEach((argName, arg) -> {
                            if (fixedArgs.containsKey(argName)) {
                                columnArgMap.put(argName, fixedArgs.get(argName));
                            } else if (callingColumnArgs.containsKey(argName)) {
                                columnArgMap.put(argName, callingColumnArgs.get(argName));
                            } else {
                                columnArgMap.put(argName, arg);
                            }
                        });

        return columnArgMap;
    }
}
