/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromArgumentSet;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static java.util.Collections.emptyMap;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Context for resolving handlebars.
 */
@Getter
@ToString
public abstract class Context {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";
    private static final String HANDLEBAR_PREFIX = "{{";
    private static final String HANDLEBAR_SUFFIX = "}}";

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
     * Resolves the handebars in column's expression.
     * @return fully resolved column's expression.
     */
    public abstract String resolve();

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     */
    public String resolve(String expression) {
        return resolveHandlebars(this, expression);
    }

    protected abstract Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException;

    protected String resolveHandlebars(Context context, String expression) {

        com.github.jknack.handlebars.Context ctx = com.github.jknack.handlebars.Context.newBuilder(context)
                        .resolver(new ContextResolver(), new StringValueResolver())
                        .build();

        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(ctx);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static Map<String, Argument> getDefaultArgumentsMap(
                    Set<com.yahoo.elide.datastores.aggregation.metadata.models.Argument> availableArgs) {

         Set<Argument> arguments = availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .map(arg -> Argument.builder()
                                        .name(arg.getName())
                                        .value(arg.getDefaultValue())
                                        .build())
                        .collect(Collectors.toSet());
         return getArgumentMapFromArgumentSet(arguments);
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

    protected static Map<String, Object> getColArgMap(ColumnProjection column, Map<String, Argument> queriedColArgs) {
        Map<String, Object> colArgsMap = new HashMap<>();
        if (column == null) {
            colArgsMap.put(ARGS_KEY, queriedColArgs);
        } else {
            colArgsMap.put(ARGS_KEY, column.getArguments());
        }

        return colArgsMap;
    }

    public static Map<String, Argument> getColumnArgMap(MetaDataStore metaDataStore,
                                                        Queryable queryable,
                                                        Map<String, Argument> queriedColArgs,
                                                        String columnName,
                                                        Map<String, Argument> fixedArgs) {

        Map<String, Argument> columnArgMap = new HashMap<>();

        Table table = metaDataStore.getTable(queryable.getSource().getName(),
                                             queryable.getSource().getVersion());

        // Add the default column argument values from metadata store.
        if (table != null && table.getColumnMap().containsKey(columnName)) {
            columnArgMap.putAll(getDefaultArgumentsMap(table.getColumnMap().get(columnName).getArguments()));
        }

        // Override default arguments with queried column's args.
        columnArgMap.putAll(queriedColArgs);
        // Any fixed arguments provided in sql helper gets preference.
        columnArgMap.putAll(fixedArgs);

        return columnArgMap;
    }

    protected class ContextResolver implements ValueResolver {

        @Override
        public Object resolve(Object context, String name) {
            if (context instanceof Context) {
                return ((Context) context).get(name);
            }
            return UNRESOLVED;
        }

        @Override
        public Object resolve(Object context) {
            return null;
        }

        @Override
        public Set<Entry<String, Object>> propertySet(Object context) {
            return null;
        }
    }

    /**
     * This tells compiler how to write a {@link StringValue} object as String.
     */
    @AllArgsConstructor
    @Getter
    protected class StringValue {
        private final String value;

        @Override
        public String toString() {
            // Sql helper returns the value already enclosed inside '{{' and '}}'
            if (value.startsWith(HANDLEBAR_PREFIX)) {
                return value;
            }
            return HANDLEBAR_PREFIX + value + HANDLEBAR_SUFFIX;
        }
    }

    /**
     * If get method returns a {@link StringValue} object. This class tell how to resolve a field
     * against {@link StringValue} object.
     * eg: To resolve {{join.col1}}, get method is called with key 'join' first,
     *     This can return a {@link StringValue} object which wraps the value 'join'.
     *     Now 'col1' is resolved against this {@link StringValue} object.
     */
    protected class StringValueResolver implements ValueResolver {

        @Override
        public Object resolve(Object context, String name) {
            if (context instanceof StringValue) {
                String value = ((StringValue) context).getValue() + PERIOD + name;

                return new StringValue(value);
            }
            return UNRESOLVED;
        }

        @Override
        public Object resolve(Object context) {
            return null;
        }

        @Override
        public Set<Entry<String, Object>> propertySet(Object context) {
            return null;
        }
    }
}
