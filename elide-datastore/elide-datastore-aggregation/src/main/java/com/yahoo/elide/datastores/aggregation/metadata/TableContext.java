/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentsFromString;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static java.util.Collections.emptyMap;

import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Helper;
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
                    .registerHelper("sql", new Helper<Object>() {
                        @Override
                        public Object apply(final Object context, final Options options) throws IOException {
                            return resolveSQLHandlebar(context, options);
                        }
                    });

    public TableContext(MetaDataStore metaDataStore, Queryable queryable, String alias) {
        this.metaDataStore = metaDataStore;
        this.queryable = queryable;
        this.alias = alias;
    }

    public TableContext(TableContext tableCtx) {
        this.metaDataStore = tableCtx.getMetaDataStore();
        this.queryable = tableCtx.getQueryable();
        this.alias = tableCtx.getAlias();
    }

    public TableContext withTableArgs(Map<String, ? extends Object> tableArgs) {
        Map<String, Object> argsMap = new HashMap<>();
        this.put("$$table", argsMap);
        argsMap.put("args", tableArgs);
        return this;
    }

    public TableContext withColumnArgs(Map<String, ? extends Object> columnArgs) {
        Map<String, Object> argsMap = new HashMap<>();
        this.put("$$column", argsMap);
        argsMap.put("args", columnArgs);
        return this;
    }

    public Object get(Object key) {

        String keyStr = key.toString();

        // Physical References starts with $
        if (keyStr.lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, queryable.getConnectionDetails().getDialect());
        }

        if (this.queryable.getJoins().containsKey(key)) {
            SQLJoin sqlJoin = this.queryable.getJoins().get(key);
            Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
            TableContext newCtx = TableContext.builder()
                            .queryable(joinQueryable)
                            .alias(appendAlias(alias, keyStr))
                            .metaDataStore(metaDataStore)
                            .build();

            // Copy $$column, $$user, $$table etc to join context.
            newCtx.putAll(this);

            return newCtx;
        }

        ColumnProjection columnProj = this.queryable.getColumnProjection(keyStr);
        if (columnProj != null) {
            return resolveHandlebars(keyStr, columnProj.getExpression(), emptyMap());
        }

        Object value = super.get(key);
        if (value != null) {
            return value;
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }

    /**
     * Resolves the handebars in column expression.
     *
     * @param columnName column's name.
     * @param columnExpr column's definition.
     * @param fixedArgs If this is called from SQL helper then pinned arguments.
     * @return fully resolved column's expression.
     */
    @SuppressWarnings("unchecked")
    public String resolveHandlebars(String columnName, String columnExpr, Map<String, Object> fixedArgs) {

        Map<String, Object> defaultTableArgs;
        Map<String, Object> defaultColumnArgs;
        Map<String, ? extends Object> tableArgsMap;
        Map<String, ? extends Object> columnArgsMap;
        Map<String, Object> newCtxTableArgs = new HashMap<>();
        Map<String, Object> newCtxColumnArgs = new HashMap<>();

        Queryable queryable = this.getQueryable();
        Table table = metaDataStore.getTable(queryable.getName(), queryable.getVersion());

        // Add the default argument values stored in metadata store.
        if (table != null) {
            defaultTableArgs = getDefaultArgumentsMap(table.getArguments());
            defaultColumnArgs = getDefaultArgumentsMap(table.getColumnMap().get(columnName).getArguments());
        } else {
            defaultTableArgs = new HashMap<>();
            defaultColumnArgs = new HashMap<>();
        }

        // Get the args under $$table.args
        if (this.containsKey(TBL_PREFIX)) {
            Map<String, Object> requestContext = (Map<String, Object>) this.get(TBL_PREFIX);
            tableArgsMap = (Map<String, ? extends Object>) requestContext.get(ARGS_KEY);
        } else {
            tableArgsMap = new HashMap<>();
        }

        // Get the args under $$column.args
        if (this.containsKey(COL_PREFIX)) {
            Map<String, Object> columnContext = (Map<String, Object>) this.get(COL_PREFIX);
            columnArgsMap = (Map<String, ? extends Object>) columnContext.get(ARGS_KEY);
        } else {
            columnArgsMap = new HashMap<>();
        }

        // Finalize table arguments, first add default table arguments and then add request table arguments.
        newCtxTableArgs.putAll(defaultTableArgs);
        newCtxTableArgs.putAll(tableArgsMap);

        // Finalize column arguments, first add default column arguments and then add request column arguments and then
        // add any fixed arguments provided in sql helper.
        newCtxColumnArgs.putAll(defaultColumnArgs);
        newCtxColumnArgs.putAll(columnArgsMap);
        newCtxColumnArgs.putAll(fixedArgs);

        // Build a new Context for resolving this column
        TableContext newCtx = new TableContext(this).withTableArgs(newCtxTableArgs).withColumnArgs(newCtxColumnArgs);

        try {
            Template template = handlebars.compileInline(columnExpr);
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

        TableContext currentTableCtx = (TableContext) context;
        // 'from' is optional, so if not provided use the same table context.
        TableContext invokedTableCtx = StringUtils.isEmpty(from) ? currentTableCtx
                                                                 : (TableContext) currentTableCtx.get(from);

        // Physical References starts with $
        if (invokedColumnName.lastIndexOf('$') == 0) {
            return invokedTableCtx.get(invokedColumnName);
        }

        Map<String, Object> pinnedArgs = new HashMap<>();
        if (argsIndex >= 0) {
            getArgumentsFromString(column.substring(argsIndex)).forEach(arg -> {
                pinnedArgs.put(arg.getName(), arg.getValue());
            });
            invokedColumnName = column.substring(0, argsIndex);
        }

        Queryable invokedQueryable = invokedTableCtx.getQueryable();

        // Assumption: sql helper will be part of SQLTable only, so table won't be null.
        SQLTable table = (SQLTable) metaDataStore.getTable(invokedQueryable.getName(), invokedQueryable.getVersion());
        String invokedColumnExpr = table.getColumnMap().get(invokedColumnName).getExpression();

        return invokedTableCtx.resolveHandlebars(invokedColumnName, invokedColumnExpr, pinnedArgs);
    }

    private static Map<String, Object> getDefaultArgumentsMap(Set<Argument> availableArgs) {

        return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .collect(Collectors.toMap(Argument::getName, Argument::getDefaultValue));
    }
}
