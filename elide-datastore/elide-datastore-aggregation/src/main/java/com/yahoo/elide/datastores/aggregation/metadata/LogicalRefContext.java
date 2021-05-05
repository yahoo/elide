/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.ValueResolver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Context for resolving arguments and logical references in column's expression. Keeps physical and join references as
 * is.
 */
@Getter
@ToString
@Builder
public class LogicalRefContext extends Context {

    private static final String HANDLEBAR_PREFIX = "{{";
    private static final String HANDLEBAR_SUFFIX = "}}";

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;

    // Arguments provided for queried column.
    private final Map<String, ? extends Object> queriedColArgs;
    private final Map<String, Object> columnArgsMap;

    @Override
    protected Object get(Object key, Map<String, ? extends Object> fixedArgs) {

        String keyStr = key.toString();

        // Keep Physical References as is
        if (keyStr.lastIndexOf('$') == 0) {
            return new StringValue(keyStr);
        }

        if (keyStr.equals(TBL_PREFIX)) {
            return getTableArgMap(this.queryable);
        }

        if (keyStr.equals(COL_PREFIX)) {
            return this.columnArgsMap;
        }

        // Keep Join References as is
        if (this.queryable.hasJoin(keyStr)) {
            return new StringValue(keyStr);
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {
            return resolve(columnProj, fixedArgs);
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    @Override
    protected String resolve(ColumnProjection column, Map<String, ? extends Object> fixedArgs) {

        // Build a new Context for resolving this column
        LogicalRefContext newCtx = LogicalRefContext.builder()
                        .queryable(queryable)
                        .metaDataStore(this.getMetaDataStore())
                        .queriedColArgs(this.getQueriedColArgs())
                        .availableColArgs(getColumnArgMap(this.getMetaDataStore(),
                                                          this.getQueryable(),
                                                          this.getQueriedColArgs(),
                                                          column.getName(),
                                                          fixedArgs))
                        .build();

        com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(newCtx)
                        .resolver(new ContextResolver(), new StringValueResolver())
                        .build();

        return resolveHandlebars(context, column.getExpression());
    }

    @Override
    protected Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String column = options.hash("column");
        int argsIndex = column.indexOf('[');
        String invokedColumnName = column;

        // Keep Join References as is
        if (!isBlank(from)) {
            return new StringValue(options.fn.text());
        }

        LogicalRefContext invokedCtx = (LogicalRefContext) context;

        if (argsIndex >= 0) {
            Map<String, ? extends Object> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }

        return invokedCtx.get(invokedColumnName);
    }

    public static class LogicalRefContextBuilder {

        public LogicalRefContextBuilder availableColArgs(final Map<String, Object> availableColArgs) {
            Map<String, Object> colArgsMap = new HashMap<>();
            colArgsMap.put(ARGS_KEY, availableColArgs);
            this.columnArgsMap = colArgsMap;
            return this;
        }
    }

    /**
     * This tells compiler how to write a {@link StringValue} object as String.
     */
    @AllArgsConstructor
    @Getter
    private class StringValue {
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
     * If get method encounters either join key, it returns a {@link StringValue} object.
     * This class tell how to resolve a field against {@link StringValue} object.
     * eg: To resolve {{join.col1}}, get method is called with key 'join' first,
     *     This will return a {@link StringValue} object which wraps the value 'join'.
     *     Now 'col1' is resolved against this {@link StringValue} object.
     */
    private class StringValueResolver implements ValueResolver {

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
