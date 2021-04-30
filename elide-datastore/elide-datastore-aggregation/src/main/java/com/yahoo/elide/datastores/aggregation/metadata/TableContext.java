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

import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import org.apache.commons.lang3.StringUtils;
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
public class TableContext extends HashMap<String, Object> {

    public static final String COL_PREFIX = "$$column";
    public static final String REQ_PREFIX = "$$request";
    public static final String TBL_PREFIX = "$$table";
    public static final String USER_PREFIX = "$$user";
    public static final String ARGS_KEY = "args";
    public static final String TABLE_KEY = "table";
    public static final String COLUMNS_KEY = "columns";
    public static final String NAME_KEY = "name";
    public static final String DOUBLE_DOLLAR = "$$";

    private final MetaDataStore metaDataStore;
    private final Queryable queryable;
    private final String alias;

    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .with((Formatter) (value, next) -> {
                if (value instanceof com.yahoo.elide.core.request.Argument) {
                    return ((com.yahoo.elide.core.request.Argument) value).getValue();
                }
                return next.format(value);
            })
            .registerHelper("sql", this::resolveSQLHandlebar);

    public TableContext withTableArgs(Map<String, ? extends Object> tableArgs) {
        Map<String, Object> argsMap = new HashMap<>();
        this.put(TBL_PREFIX, argsMap);
        argsMap.put(ARGS_KEY, tableArgs);
        return this;
    }

    public TableContext withColumnArgs(Map<String, ? extends Object> columnArgs) {
        Map<String, Object> argsMap = new HashMap<>();
        this.put(COL_PREFIX, argsMap);
        argsMap.put(ARGS_KEY, columnArgs);
        return this;
    }

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
            TableContext newCtx = TableContext.builder()
                            .queryable(joinQueryable)
                            .alias(appendAlias(alias, keyStr))
                            .metaDataStore(metaDataStore)
                            .build();

            // Copy $$column to join context.
            if (this.containsKey(COL_PREFIX)) {
                newCtx.put(COL_PREFIX, this.get(COL_PREFIX));
            }
            return newCtx;
        }

        Object value = super.get(key);
        if (value != null) {
            return value;
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {
            // Use the args under $$column.args
            return resolveHandlebars(keyStr, columnProj.getExpression(), getArgsFromContext(COL_PREFIX), fixedArgs);
        }
        // Assumption: Non-Projected column must be projected in Query's source.
        // TODO: Currently everything is not projected in Query, Remove this case once everything is projected in
        // Query (https://github.com/yahoo/elide/issues/2018).
        Queryable source = this.queryable.getSource();

        if (source.getColumnProjection(keyStr) == null) {
            throw new HandlebarsException(new Throwable("Couldn't find: " + key));
        }

        TableContext newCtx = TableContext.builder()
                        .queryable(source)
                        .alias(source.getAlias())
                        .metaDataStore(metaDataStore)
                        .build();

        // Copy $$table & $$column to context for source.
        newCtx.put(COL_PREFIX, this.get(COL_PREFIX));
        newCtx.put(TBL_PREFIX, this.get(TBL_PREFIX));
        return newCtx.get(keyStr);
    }

    /**
     * Resolves the handebars in column expression.
     *
     * @param columnName column's name.
     * @param columnExpr expression to resolve.
     * @param columnArgsMap column's arguments.
     * @return fully resolved column's expression.
     */
    public String resolveHandlebars(String columnName, String columnExpr, Map<String, ? extends Object> columnArgsMap) {
        return resolveHandlebars(columnName, columnExpr, columnArgsMap, emptyMap());
    }

    /**
     * Resolves the handebars in column expression.
     *
     * @param columnName column's name.
     * @param columnExpr column's definition.
     * @param columnArgsMap column's arguments.
     * @param fixedArgs If this is called from SQL helper then pinned arguments.
     * @return fully resolved column's expression.
     */
    private String resolveHandlebars(String columnName, String columnExpr, Map<String, ? extends Object> columnArgsMap,
                    Map<String, ? extends Object> fixedArgs) {

        Map<String, Object> defaultTableArgs = null;
        Map<String, Object> defaultColumnArgs = null;
        Map<String, ? extends Object> tableArgsMap = null;
        Map<String, Object> newCtxTableArgs = new HashMap<>();
        Map<String, Object> newCtxColumnArgs = new HashMap<>();

        Queryable queryable = this.getQueryable();
        Table table = metaDataStore.getTable(queryable.getSource().getName(), queryable.getSource().getVersion());

        // Add the default argument values stored in metadata store.
        if (table != null) {
            defaultTableArgs = getDefaultArgumentsMap(table.getArguments());
            Column column = table.getColumnMap().get(columnName);
            if (column != null) {
                defaultColumnArgs = getDefaultArgumentsMap(column.getArguments());
            }
        }

        if (queryable instanceof Query) {
            Query query = (Query) queryable;
            tableArgsMap = query.getArguments();
        }

        /**
         * Finalize table arguments:
         * i) Add default arguments for this Queryable.
         * ii) Override default arguments with either a) Query Args if available OR b) Table arguments in context.
         */
        newCtxTableArgs.putAll(defaultTableArgs == null ? emptyMap() : defaultTableArgs);
        newCtxTableArgs.putAll(tableArgsMap == null ? getArgsFromContext(TBL_PREFIX) : tableArgsMap);

        /**
         * Finalize column arguments:
         * i) Add default arguments for this column.
         * ii) Override default arguments with calling column's args. When this method is called from
         *  SQLColumnProjection#toSql, then column's arguments with in query are used instead.
         * iii) Any fixed arguments provided in sql helper get preference.
         */
        newCtxColumnArgs.putAll(defaultColumnArgs == null ? emptyMap() : defaultColumnArgs);
        newCtxColumnArgs.putAll(columnArgsMap == null ? emptyMap() : columnArgsMap);
        newCtxColumnArgs.putAll(fixedArgs == null ? emptyMap() : fixedArgs);

        // Build a new Context for resolving this column
        TableContext newCtx = TableContext.builder()
                        .queryable(queryable)
                        .alias(this.getAlias())
                        .metaDataStore(this.getMetaDataStore())
                        .build()
                        .withTableArgs(newCtxTableArgs)
                        .withColumnArgs(newCtxColumnArgs);

        Context context = Context.newBuilder(newCtx).build();
        try {
            Template template = handlebars.compileInline(columnExpr);
            return template.apply(context);
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

        TableContext currentTableCtx = (TableContext) context;
        // 'from' is optional, so if not provided use the same table context.
        TableContext invokedTableCtx = StringUtils.isEmpty(from) ? currentTableCtx
                                                                 : (TableContext) currentTableCtx.get(from);

        // Physical References starts with $
        if (invokedColumnName.lastIndexOf('$') == 0) {
            return invokedTableCtx.get(invokedColumnName);
        }

        if (argsIndex >= 0) {
            Map<String, ? extends Object> pinnedArgs = getArgumentMapFromString(column.substring(argsIndex));
            invokedColumnName = column.substring(0, argsIndex);
            return invokedTableCtx.get(invokedColumnName, pinnedArgs);
        }
        return invokedTableCtx.get(invokedColumnName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ? extends Object> getArgsFromContext(String outerKey) {
        Map<String, Object> map = (Map<String, Object>) this.getOrDefault(outerKey, emptyMap());
        return (Map<String, ? extends Object>) map.getOrDefault(ARGS_KEY, emptyMap());
    }

    public static Map<String, Object> getDefaultArgumentsMap(Set<Argument> availableArgs) {

        return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .collect(Collectors.toMap(Argument::getName, Argument::getDefaultValue));
    }
}
