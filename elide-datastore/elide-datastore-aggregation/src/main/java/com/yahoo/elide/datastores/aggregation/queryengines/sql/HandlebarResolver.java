/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.core.filter.dialect.RSQLFilterDialect.FILTER_ARGUMENTS_PATTERN;
import static com.yahoo.elide.core.utils.TypeHelper.nullOrEmpty;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.ARGS_PREFIX;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.COL_PREFIX;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.prepareArgumentsMap;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class HandlebarResolver {

    private final Handlebars handlebars = new Handlebars()
                    .with(EscapingStrategy.NOOP)
                    .registerHelper("sql", new Helper<Object>() {

                        @Override
                        public Object apply(final Object context, final Options options) throws IOException {
                            String from = options.hash("from");
                            String columnName = options.hash("column");
                            int argsIndex = columnName.indexOf('[');

                            TableContext currentTableCtx = (TableContext) context;
                            TableContext invokedTableCtx = nullOrEmpty(from) ? currentTableCtx
                                                                             : (TableContext) currentTableCtx.get(from);

                            // Physical References starts with $
                            if (columnName.lastIndexOf('$') == 0) {
                                return invokedTableCtx.get(columnName);
                            }

                            Map<String, Object> pinnedArgs = new HashMap<>();
                            if (argsIndex >= 0) {
                                parseArguments(columnName.substring(argsIndex), pinnedArgs);
                                columnName = columnName.substring(0, argsIndex);
                            }

                            return resolveInvokeColumn(columnName, currentTableCtx, invokedTableCtx, pinnedArgs);
                        }
                    });

    public String resolveHandlebars(TableContext tableCtx, String columnName, ColumnDefinition columnDef) {

        Map<String, Object> columnArgsContext = columnDef.getDefaultColumnArgs();
        Map<String, Object> tableArgsContext = tableCtx.getDefaultTableArgs();
        // TODO: Merge with arguments from Request Context

        // Build a new Context for resolving this column
        TableContext newCtx = TableContext.builder()
                        .alias(tableCtx.getAlias())
                        .dialect(tableCtx.getDialect())
                        .defaultTableArgs(tableCtx.getDefaultTableArgs())
                        .joins(tableCtx.getJoins())
                        .build();
        newCtx.putAll(tableCtx);

        newCtx.putAll(tableArgsContext);
        newCtx.putAll(columnArgsContext);

        try {
            Template template = handlebars.compileInline(columnDef.getExpression());
            return template.apply(newCtx);
        } catch (IOException e) {
            // Do Nothing
            return null;
        }
    }

    private void parseArguments(String argsString, Map<String, Object> pinnedArgs) throws UnsupportedEncodingException {
        if (argsString == null || argsString.isEmpty()) {
            return;
        }

        Matcher matcher = FILTER_ARGUMENTS_PATTERN.matcher(argsString);
        while (matcher.find()) {
            pinnedArgs.put(matcher.group(1),
                           URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8.name()));
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveInvokeColumn(String columnName, TableContext currentTableCtx,
                    TableContext invokedTableCtx, Map<String, Object> pinnedArgs) {
        ColumnDefinition invokedColumnOrig = invokedTableCtx.getColumnDefinition(columnName);

        // Build a new Context for resolving this invoked column
        TableContext newCtx = TableContext.builder()
                        .alias(invokedTableCtx.getAlias())
                        .dialect(invokedTableCtx.getDialect())
                        .defaultTableArgs(invokedTableCtx.getDefaultTableArgs())
                        .joins(invokedTableCtx.getJoins())
                        .build();
        newCtx.putAll(invokedTableCtx);

        // Build a new ColumnDefinition overriding arguments
        Map<String, Object> defaultInvokedColumnArgs = (Map<String, Object>) ((Map<String, Object>) invokedColumnOrig
                        .getDefaultColumnArgs().get(COL_PREFIX)).get(ARGS_PREFIX);
        Map<String, Object> currentColumnArgs =
                        (Map<String, Object>) ((Map<String, Object>) currentTableCtx.get(COL_PREFIX)).get(ARGS_PREFIX);
        Map<String, Object> invokedColumnArgs = new HashMap<>();
        invokedColumnArgs.putAll(defaultInvokedColumnArgs);
        invokedColumnArgs.putAll(currentColumnArgs);
        invokedColumnArgs.putAll(pinnedArgs);

        ColumnDefinition invokedColumn = new ColumnDefinition(invokedColumnOrig.getExpression(),
                                                              prepareArgumentsMap(invokedColumnArgs, COL_PREFIX));
        newCtx.put(columnName, invokedColumn);

        return resolveHandlebars(newCtx, columnName, invokedColumn);
    }
}
