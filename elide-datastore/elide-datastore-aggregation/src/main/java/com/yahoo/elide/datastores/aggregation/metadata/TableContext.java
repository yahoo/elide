/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.filter.dialect.RSQLFilterDialect.FILTER_ARGUMENTS_PATTERN;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static java.util.Collections.emptyMap;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
    public static final String ARGS_KEY = "args";
    public static final String TABLE_KEY = "table";
    public static final String COLUMNS_KEY = "columns";
    public static final String NAME_KEY = "name";

    private final MetaDataStore metaDataStore;

    private final Queryable queryable;
    private final String alias;

    @Builder.Default
    private final Map<String, Queryable> joins = new HashMap<>();

    private final Handlebars handlebars = new Handlebars()
                    .with(EscapingStrategy.NOOP)
                    .registerHelper("sql", new Helper<Object>() {
                        @Override
                        public Object apply(final Object context, final Options options) throws IOException {
                            return resolveSQLHandlebar(context, options);
                        }
                    });

    public Object get(Object key) {

        if (joins.containsKey(key)) {
            Queryable joinQueryable = joins.get(key);
            TableContext newCtx = TableContext.builder()
                            .queryable(joinQueryable)
                            .alias(appendAlias(alias, key.toString()))
                            .metaDataStore(metaDataStore)
                            .build();

            boolean isNested = joinQueryable.isNested();
            joinQueryable.getColumnProjections().forEach(column -> {
                if (!isNested && column.getColumnType() == ColumnType.FIELD) {
                    newCtx.put(column.getName(), "{{$" + column.getExpression() + "}}");
                } else {
                    newCtx.put(column.getName(), column.getExpression());
                }
            });

            joinQueryable.getJoins().forEach((name, join) -> {
                SQLTable joinTable = metaDataStore.getTable(join.getJoinTableType());
                newCtx.addJoin(name, joinTable);
            });

            return newCtx;
        }

        // Physical References starts with $
        if (key.toString().lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, queryable.getConnectionDetails().getDialect());
        }

        verifyKeyExists(key, super.keySet());

        Object value = super.get(key);
        if (key.toString().startsWith("$$")) {
            return value;
        }

        return resolveHandlebars(this, key.toString(), value.toString(), emptyMap());
    }

    /**
     * Resolves the handebars in column expression.
     *
     * @param tableCtx current {@link TableContext}.
     * @param columnName column's name.
     * @param columnExpr column's definition.
     * @param callingColumnArgs If this is called from SQL helper then calling column's arguments.
     * @return fully resolved column's expression.
     */
    @SuppressWarnings("unchecked")
    public String resolveHandlebars(TableContext tableCtx, String columnName, String columnExpr,
                    Map<String, Object> callingColumnArgs) {

        Map<String, Object> tableArgs = new HashMap<>();
        Map<String, Object> columnArgs = new HashMap<>();
        Map<String, Object> requestTableArgs = new HashMap<>();
        Map<String, Object> requestColumnArgs = new HashMap<>();

        Queryable queryable = tableCtx.getQueryable();
        Table table = metaDataStore.getTable(queryable.getName(), queryable.getVersion());

        // Add the default argument values stored in metadata store.
        if (table != null) {
            tableArgs.putAll(getDefaultArgumentsMap(table.getArguments()));
            columnArgs.putAll(getDefaultArgumentsMap(table.getColumnMap().get(columnName).getArguments()));
        }

        // Get the map stored under $$request.table.
        Map<String, Object> requestTableMap = (Map<String, Object>)
                        ((Map<String, Object>) tableCtx.getOrDefault(REQ_PREFIX, emptyMap()))
                        .getOrDefault(TABLE_KEY, emptyMap());

        // If $$request.table.name matches current Queryable
        if (!requestTableMap.isEmpty() && requestTableMap.get(NAME_KEY).equals(tableCtx.getQueryable().getName())) {
            requestTableArgs = (Map<String, Object>) requestTableMap.get(ARGS_KEY);

            // Get the map stored under $$request.columns.
            Map<String, Object> requestColumnsMap = (Map<String, Object>)
                            ((Map<String, Object>) tableCtx.getOrDefault(REQ_PREFIX, emptyMap()))
                            .getOrDefault(COLUMNS_KEY, emptyMap());

            // If $$request.columns.columnName matches current column Name
            if (requestColumnsMap.containsKey(columnName)) {
                requestColumnArgs = (Map<String, Object>)
                                ((Map<String, Object>) requestColumnsMap.get(columnName))
                                .get(ARGS_KEY);
            }
        }

        // Override default table and column arguments with arguments provided in request.
        tableArgs.putAll(requestTableArgs);
        columnArgs.putAll(requestColumnArgs);

        // If this is invoked using SQL helper, calling column's arguments must override current column's default and
        // request arguments.
        columnArgs.putAll(callingColumnArgs);

        // Build a new Context for resolving this column
        TableContext newCtx = TableContext.builder()
                        .queryable(queryable)
                        .alias(tableCtx.getAlias())
                        .joins(tableCtx.getJoins())
                        .metaDataStore(metaDataStore)
                        .build();
        newCtx.putAll(tableCtx);

        newCtx.putAll(prepareArgumentsMap(tableArgs, TBL_PREFIX));
        newCtx.putAll(prepareArgumentsMap(columnArgs, COL_PREFIX));

        try {
            Template template = handlebars.compileInline(columnExpr);
            return template.apply(newCtx);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
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
            parseArguments(column.substring(argsIndex), pinnedArgs);
            invokedColumnName = column.substring(0, argsIndex);
        }

        Queryable invokedQueryable = invokedTableCtx.getQueryable();

        // Assumption: sql helper will be part of SQLTable only, so table won't be null.
        SQLTable table = (SQLTable) metaDataStore.getTable(invokedQueryable.getName(), invokedQueryable.getVersion());
        String invokedColumnExpr = table.getColumnMap().get(invokedColumnName).getExpression();

        Map<String, Object> currentColumnArgs = (Map<String, Object>)
                        ((Map<String, Object>) currentTableCtx.get(COL_PREFIX))
                        .get(ARGS_KEY);

        // Ideally pinned arguments wont clash with column arguments
        // But if it happens, column arguments gets preference so overriding pinned arguments.
        pinnedArgs.putAll(currentColumnArgs);

        return resolveHandlebars(invokedTableCtx, invokedColumnName, invokedColumnExpr, pinnedArgs);
    }

    public void addJoin(String joinName, Queryable joinQueryable) {
        this.joins.put(joinName, joinQueryable);
    }

    private void verifyKeyExists(Object key, Set<String> keySet) {
        if (!keySet.contains(key)) {
            throw new HandlebarsException(new Throwable("Couldn't find: " + key));
        }
    }

    private static void parseArguments(String argsString, Map<String, Object> pinnedArgs)
                    throws UnsupportedEncodingException {
        if (argsString == null || argsString.isEmpty()) {
            return;
        }

        Matcher matcher = FILTER_ARGUMENTS_PATTERN.matcher(argsString);
        while (matcher.find()) {
            pinnedArgs.put(matcher.group(1),
                           URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8.name()));
        }
    }

    private static Map<String, Object> prepareArgumentsMap(Map<String, Object> arguments, String outerMapKey) {

        Map<String, Object> outerArgsMap = new HashMap<>();
        Map<String, Object> argsMap = new HashMap<>();
        outerArgsMap.put(outerMapKey, argsMap);
        argsMap.put(ARGS_KEY, arguments);

        return outerArgsMap;
    }

    private static Map<String, Object> getDefaultArgumentsMap(Set<Argument> availableArgs) {

        return availableArgs.stream()
                        .filter(arg -> arg.getDefaultValue() != null)
                        .collect(Collectors.toMap(Argument::getName, Argument::getDefaultValue));
    }
}
