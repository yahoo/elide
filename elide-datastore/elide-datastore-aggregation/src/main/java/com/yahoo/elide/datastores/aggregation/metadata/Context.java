/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import org.apache.commons.lang3.tuple.Pair;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TableContext for Handlebars Resolution.
 */
@Getter
@ToString
@Builder
public class Context extends HashMap<String, Object> {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;
    private final String alias;

    // Arguments provided for queried column.
    private final Map<String, ? extends Object> queriedColArgs;

    // Default arguments for current column + Arguments provided for queried column.
    private final Map<String, Object> availableColArgs;

    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .with((Formatter) (value, next) -> {
                if (value instanceof com.yahoo.elide.core.request.Argument) {
                    return ((com.yahoo.elide.core.request.Argument) value).getValue();
                }
                return next.format(value);
            })
            .registerHelper("sql", this::resolveSQLHandlebar);

    @Override
    public Object get(Object key) {
        return get(key, emptyMap());
    }

    private Object get(Object key, Map<String, ? extends Object> fixedArgs) {

        String keyStr = key.toString();

        // Physical References starts with $
        if (keyStr.lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, queryable.getConnectionDetails().getDialect());
        }

        if (this.queryable.hasJoin(keyStr)) {
            SQLJoin sqlJoin = this.queryable.getJoin(keyStr);
            Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            Context joinCtx = Context.builder()
                            .queryable(joinQueryable)
                            .alias(appendAlias(this.alias, keyStr))
                            .metaDataStore(this.metaDataStore)
                            .queriedColArgs(this.queriedColArgs)
                            .build();

            return joinCtx;
        }

        Object value = super.get(key);
        if (value != null) {
            return value;
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {
            return resolveHandlebars(columnProj, fixedArgs);
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    /**
     * Resolves the handebars in column's expression.
     *
     * @param column {@link ColumnProjection}
     * @return fully resolved column's expression.
     */
    public String resolveHandlebars(ColumnProjection column) {
        return resolveHandlebars(column, emptyMap());
    }

    /**
     * Resolves the handebars in column's expression.
     *
     * @param column {@link ColumnProjection}
     * @param fixedArgs If this is called from SQL helper then pinned arguments.
     * @return fully resolved column's expression.
     */
    private String resolveHandlebars(ColumnProjection column, Map<String, ? extends Object> fixedArgs) {

        Map<String, Object> newCtxColumnArgs = new HashMap<>();

        Queryable queryable = this.getQueryable();
        Table table = metaDataStore.getTable(queryable.getSource().getName(), queryable.getSource().getVersion());

        // Add the default column argument values from metadata store.
        if (table != null) {
            Column col = table.getColumnMap().get(column.getName());
            if (col != null) {
                newCtxColumnArgs.putAll(getDefaultArgumentsMap(col.getArguments()));
            }
        }

        // Override default arguments with queried column's args.
        newCtxColumnArgs.putAll(this.getQueriedColArgs());
        // Any fixed arguments provided in sql helper get preference.
        newCtxColumnArgs.putAll(fixedArgs == null ? emptyMap() : fixedArgs);

        // Build a new Context for resolving this column
        Context newCtx = Context.builder()
                        .queryable(queryable)
                        .alias(this.getAlias())
                        .metaDataStore(this.getMetaDataStore())
                        .queriedColArgs(this.getQueriedColArgs())
                        .availableColArgs(newCtxColumnArgs)
                        .build();

        try {
            Template template = handlebars.compileInline(column.getExpression());
            return template.apply(newCtx);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String column = options.hash("column");
        int argsIndex = column.indexOf('[');
        String invokedColumnName = column;

        Context currentCtx = (Context) context;
        // 'from' is optional, so if not provided use the same table context.
        Context invokedCtx = isBlank(from) ? currentCtx
                                           : (Context) currentCtx.get(from);

        // Physical References starts with $
        if (invokedColumnName.lastIndexOf('$') == 0) {
            return invokedCtx.get(invokedColumnName);
        }

        if (argsIndex >= 0) {
            Map<String, ? extends Object> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedCtx.get(invokedColumnName, pinnedArgs);
        }
        return invokedCtx.get(invokedColumnName);
    }

    /**
     * Resolves the handlebars with in expression.
     * @param columnName Name of the column whose default arguments to be used for resolving this expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     */
    public String resolveHandlebars(String columnName, String expression) {
        ColumnProjection column = new ColumnProjection() {
            @Override
            public <T extends ColumnProjection> T withProjected(boolean projected) {
                return null;
            }
            @Override
            public Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source, SQLReferenceTable lookupTable,
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

        return resolveHandlebars(column);
    }

    public static Map<String, Object> getDefaultArgumentsMap(Set<Argument> availableArgs) {

        return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .collect(Collectors.toMap(Argument::getName, Argument::getDefaultValue));
    }

    public static class ContextBuilder {
        public Context build() {
            Context context = new Context(this.metaDataStore, this.queryable, this.alias, this.queriedColArgs,
                            this.availableColArgs);

            Map<String, Object> tableArgs = new HashMap<>();

            Table table = this.metaDataStore.getTable(this.queryable.getSource().getName(),
                                                      this.queryable.getSource().getVersion());

            // Add the default table argument values from metadata store.
            if (table != null) {
                tableArgs.putAll(getDefaultArgumentsMap(table.getArguments()));
            }

            // Override default arguments with Query Args if available.
            if (this.queryable instanceof Query) {
                Query query = (Query) this.queryable;
                tableArgs.putAll(query.getArguments());
            }

            Map<String, Object> tblArgsMap = new HashMap<>();
            context.put(TBL_PREFIX, tblArgsMap);
            tblArgsMap.put(ARGS_KEY, tableArgs);

            if (this.availableColArgs != null) {
                Map<String, Object> colArgsMap = new HashMap<>();
                context.put(COL_PREFIX, colArgsMap);
                colArgsMap.put(ARGS_KEY, this.availableColArgs);
            }

            return context;
        }
    }
}
