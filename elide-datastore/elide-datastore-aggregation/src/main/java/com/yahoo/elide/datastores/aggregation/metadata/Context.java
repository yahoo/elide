/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static java.util.Collections.emptyMap;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
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

import org.apache.commons.lang3.tuple.Pair;

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
 * Context for resolving all handlebars in provided expression.
 */
@Getter
@ToString
public abstract class Context {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";

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

    protected abstract Object get(Object key, Map<String, ? extends Object> fixedArgs);

    /**
     * Resolves the handebars in column's expression.
     *
     * @param column {@link ColumnProjection}
     * @return fully resolved column's expression.
     */
    public String resolve(ColumnProjection column) {
        return resolve(column, emptyMap());
    }

    /**
     * Resolves the handebars in column's expression.
     *
     * @param column {@link ColumnProjection}
     * @param fixedArgs If this is called from SQL helper then pinned arguments.
     * @return fully resolved column's expression.
     */
    protected abstract String resolve(ColumnProjection column, Map<String, ? extends Object> fixedArgs);

    protected abstract Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException;

    protected String resolveHandlebars(com.github.jknack.handlebars.Context context, String expression) {
        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(context);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Resolves the handlebars with in expression.
     * @param columnName Name of the column whose default arguments to be used for resolving this expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     */
    public String resolve(String columnName, String expression) {
        ColumnProjection column = new ColumnProjection() {
            @Override
            public <T extends ColumnProjection> T withProjected(boolean projected) {
                return null;
            }
            @Override
            public Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source, MetaDataStore store,
                            boolean joinInOuter) {
                return null;
            }
            @Override
            public ValueType getValueType() {
                return null;
            }
            @Override
            public String getName() {
                return columnName;
            }
            @Override
            public String getExpression() {
                return expression;
            }
            @Override
            public ColumnType getColumnType() {
                return null;
            }
        };

        return resolve(column);
    }

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     */
    public String resolve(String expression) {

        com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context.newBuilder(this)
                        .resolver(new ContextResolver())
                        .build();
        return resolveHandlebars(context, expression);
    }

    public static Map<String, Object> getDefaultArgumentsMap(Set<Argument> availableArgs) {

        return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .collect(Collectors.toMap(Argument::getName, Argument::getDefaultValue));
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

    protected static Map<String, Object> getColumnArgMap(MetaDataStore metaDataStore,
                                                         Queryable queryable,
                                                         Map<String, ? extends Object> queriedColArgs,
                                                         String columnName,
                                                         Map<String, ? extends Object> fixedArgs) {

        Map<String, Object> columnArgMap = new HashMap<>();

        Table table = metaDataStore.getTable(queryable.getSource().getName(),
                                             queryable.getSource().getVersion());

        // Add the default column argument values from metadata store.
        if (table != null && table.getColumnMap().containsKey(columnName)) {
            Set<Argument> columnArgs = table.getColumnMap().get(columnName).getArguments();
            columnArgMap.putAll(getDefaultArgumentsMap(columnArgs));
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
}
