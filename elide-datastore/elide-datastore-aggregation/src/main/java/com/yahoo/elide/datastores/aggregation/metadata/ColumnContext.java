/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.createSafeAlias;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Formatter;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for resolving all handlebars in provided expression.
 */
@Getter
@ToString
@Builder
public class ColumnContext extends HashMap<String, Object> {

    public static final String COL_PREFIX = "$$column";
    public static final String TBL_PREFIX = "$$table";
    public static final String ARGS_KEY = "args";
    public static final String EXPR_KEY = "expr";
    public static final String PERIOD = ".";

    protected final MetaDataStore metaDataStore;
    protected final Queryable queryable;
    protected final String alias;
    protected final ColumnProjection column;
    protected final Map<String, Argument> tableArguments;

    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .with((Formatter) (value, next) -> {
                if (value instanceof com.yahoo.elide.core.request.Argument) {
                    return ((com.yahoo.elide.core.request.Argument) value).getValue();
                }
                return next.format(value);
            })
            .registerHelper("sql", this::resolveSQLHandlebar);

    public Object get(Object key) {

        String keyStr = key.toString();

        // Physical References starts with $
        if (keyStr.lastIndexOf('$') == 0) {
            return resolvePhysicalReference(this, keyStr);
        }

        if (keyStr.equals(TBL_PREFIX)) {
            return TableSubContext.tableSubContextBuilder()
                            .tableArguments(this.tableArguments)
                            .build();
        }

        if (keyStr.equals(COL_PREFIX)) {
            return ColumnSubContext.columnSubContextBuilder()
                            .queryable(this.getQueryable())
                            .alias(this.getAlias())
                            .metaDataStore(this.getMetaDataStore())
                            .column(this.getColumn())
                            .tableArguments(this.getTableArguments())
                            .build();
        }

        if (this.queryable.hasJoin(keyStr)) {
            return getJoinContext(keyStr);
        }

        // Check if key exists in Map.
        Object value = getOrDefault(key, null);
        if (value != null) {
            return value;
        }

        // In case of colA references colB and user query has both colA and colB,
        // we should use default arguments for colB while resolving colA instead of user provided argument for colB.
        // so taking colB's details from current queryable's source.
        ColumnProjection column = this.getQueryable().getSource().getColumnProjection(keyStr);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            mergedArgumentMap(column.getArguments(),
                                              this.getColumn().getArguments()));

            return getNewContext(this, newColumn).resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + keyStr));
    }

    protected String resolvePhysicalReference(ColumnContext context, String key) {
        String resolvedExpr = context.getAlias() + PERIOD + key.substring(1);
        return applyQuotes(resolvedExpr, context.getQueryable().getDialect());
    }

    protected ColumnContext getJoinContext(String key) {
        SQLJoin sqlJoin = this.queryable.getJoin(key);

        String joinExpression = sqlJoin.getJoinExpression();
        PhysicalRefColumnContext context = PhysicalRefColumnContext.physicalRefContextBuilder()
                        .queryable(this.getQueryable())
                        .metaDataStore(this.getMetaDataStore())
                        .column(this.getColumn())
                        .tableArguments(this.getTableArguments())
                        .build();

        // This will resolve everything within join expression except Physical References, Use this resolved value
        // to create alias for join dynamically.
        String resolvedJoinExpr = context.resolve(joinExpression);
        String joinAlias = createSafeAlias(appendAlias(this.alias, key), resolvedJoinExpr);

        Queryable joinQueryable = metaDataStore.getTable(sqlJoin.getJoinTableType());
        ColumnContext joinCtx = ColumnContext.builder()
                        .queryable(joinQueryable)
                        .alias(joinAlias)
                        .metaDataStore(this.metaDataStore)
                        .column(this.column)
                        .tableArguments(mergedArgumentMap(joinQueryable.getArguments(),
                                                          this.getTableArguments()))
                        .build();

        return joinCtx;
    }

    protected ColumnContext getNewContext(ColumnContext context, ColumnProjection newColumn) {
        return ColumnContext.builder()
                        .queryable(context.getQueryable())
                        .alias(context.getAlias())
                        .metaDataStore(context.getMetaDataStore())
                        .column(newColumn)
                        .tableArguments(context.getTableArguments())
                        .build();
    }

    /**
     * Resolves the handlebars with in expression.
     * @param expression Expression to resolve.
     * @return fully resolved expression.
     * @throws IllegalStateException for any handlebars error.
     */
    public String resolve(String expression) {
        try {
            Template template = handlebars.compileInline(expression);
            return template.apply(this);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private Object resolveSQLHandlebar(final Object context, final Options options)
                    throws UnsupportedEncodingException {
        String from = options.hash("from");
        String columnName = options.hash("column");
        int argsIndex = columnName.indexOf('[');
        String invokedColumnName = columnName;

        ColumnContext currentCtx = (ColumnContext) context;
        // 'from' is optional, so if not provided use the same table context.
        ColumnContext invokedCtx = isBlank(from) ? currentCtx
                                                 : (ColumnContext) currentCtx.get(from);

        Map<String, Argument> pinnedArgs = new HashMap<>();

        if (argsIndex >= 0) {
            pinnedArgs = getArgumentMapFromString(columnName.substring(argsIndex));
            invokedColumnName = columnName.substring(0, argsIndex);
        }

        // Physical References starts with $
        if (invokedColumnName.lastIndexOf('$') == 0) {
            return resolvePhysicalReference(invokedCtx, invokedColumnName);
        }

        ColumnProjection column = invokedCtx.getQueryable().getSource().getColumnProjection(invokedColumnName);
        if (column != null) {

            ColumnProjection newColumn = column.withArguments(
                            mergedArgumentMap(column.getArguments(),
                                              invokedCtx.getColumn().getArguments(),
                                              pinnedArgs));

            return getNewContext(invokedCtx, newColumn).resolve(newColumn.getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + invokedColumnName));
    }

    /**
     * Checks if referenced {@link ColumnProjection} or {@link Queryable} arguments are available in either calling
     * object's arguments or in fixed arguments. If yes, use that value. Fixed arguments gets preference over calling
     * object's arguments.
     * @param referencedObjectArgs referenced {@link ColumnProjection} or {@link Queryable} arguments.
     * @param callingObjectArgs calling {@link ColumnProjection} or {@link Queryable} arguments.
     * @param fixedArgs Fixed arguments.
     * @return Available arguments for referenced {@link ColumnProjection} or {@link Queryable}.
     */
    public static Map<String, Argument> mergedArgumentMap(Map<String, Argument> referencedObjectArgs,
                                                          Map<String, Argument> callingObjectArgs,
                                                          Map<String, Argument> fixedArgs) {

        Map<String, Argument> columnArgMap = new HashMap<>();

        referencedObjectArgs.forEach((argName, arg) -> {
            if (fixedArgs.containsKey(argName)) {
                columnArgMap.put(argName, fixedArgs.get(argName));
            } else if (callingObjectArgs.containsKey(argName)) {
                columnArgMap.put(argName, callingObjectArgs.get(argName));
            } else {
                columnArgMap.put(argName, arg);
            }
        });

        return columnArgMap;
    }

    /**
     * Checks if referenced {@link ColumnProjection} or {@link Queryable} arguments are available in calling
     * object's arguments. If yes, use that value.
     * @param referencedObjectArgs referenced {@link ColumnProjection} or {@link Queryable} arguments.
     * @param callingObjectArgs calling {@link ColumnProjection} or {@link Queryable} arguments.
     * @return Available arguments for referenced {@link ColumnProjection} or {@link Queryable}.
     */
    public static Map<String, Argument> mergedArgumentMap(Map<String, Argument> referencedObjectArgs,
                    Map<String, Argument> callingObjectArgs) {
        return mergedArgumentMap(referencedObjectArgs, callingObjectArgs, emptyMap());
    }

    /**
     * Split a string on ".", append quotes around each split and join it back.
     * eg: game.order_details to `game`.`order_details` .
     *
     * @param str column name / alias
     * @param beginQuote prefix char
     * @param endQuote suffix char
     * @return quoted string
     */
    private static String applyQuotes(String str, char beginQuote, char endQuote) {
        if (isBlank(str)) {
            return str;
        }
        if (str.contains(PERIOD)) {
            return beginQuote + str.trim().replace(PERIOD, endQuote + PERIOD + beginQuote) + endQuote;
        }
        return beginQuote + str.trim() + endQuote;
    }

    /**
     * Split a string on ".", append quotes around each split and join it back.
     * eg: game.order_details to `game`.`order_details` .
     *
     * @param str column name / alias
     * @param dialect Elide SQL dialect
     * @return quoted string
     */
    public static String applyQuotes(String str, SQLDialect dialect) {
        return applyQuotes(str, dialect.getBeginQuote(), dialect.getEndQuote());
    }
}
