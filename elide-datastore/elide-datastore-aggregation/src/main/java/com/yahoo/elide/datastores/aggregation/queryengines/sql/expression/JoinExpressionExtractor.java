/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.metadata.TableContext.getDefaultArgumentsMap;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.hasSql;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.resolveTableOrSubselect;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.TableContext;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides Join Expressions for all the {@link JoinReference} in a given reference tree.
 */
public class JoinExpressionExtractor implements ReferenceVisitor<Set<String>> {

    private static final String ON = "ON ";
    private static final String OPEN_BRACKET = "(";
    private static final String CLOSE_BRACKET = ")";

    private final Set<String> joinExpressions = new LinkedHashSet<>();

    private final TableContext tableCtx;

    /**
     * Stores all the available column arguments for any column in the reference tree.
     */
    private final Map<String, Object> availableColArgs = new HashMap<>();

    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;

    public JoinExpressionExtractor(TableContext tableCtx, Map<String, ? extends Object> currentColArgs) {
        this.tableCtx = tableCtx;
        this.availableColArgs.putAll(currentColArgs);
        this.metaDataStore = tableCtx.getMetaDataStore();
        this.dictionary = tableCtx.getMetaDataStore().getMetadataDictionary();
    }

    @Override
    public Set<String> visitPhysicalReference(PhysicalReference reference) {
        return joinExpressions;
    }

    @Override
    public Set<String> visitLogicalReference(LogicalReference reference) {

        /**
         * For the scenario: col1:{{col2}}, col2:{{col3}}, col3:{{join.col1}}
         * Current visitor will have column arguments for `col1`.
         * Creating new visitor after adding default arguments for `col2`
         */
        Map<String, Object> defaultColumnArgs = null;
        Map<String, Object> availableColArgs = new HashMap<>();

        Queryable queryable = this.tableCtx.getQueryable();
        Table table = metaDataStore.getTable(queryable.getSource().getName(), queryable.getSource().getVersion());

        if (table != null) {
            Column column = table.getColumnMap().get(reference.getColumn().getName());
            if (column != null) {
                defaultColumnArgs = getDefaultArgumentsMap(column.getArguments());
            }
        }

        availableColArgs.putAll(defaultColumnArgs == null ? emptyMap() : defaultColumnArgs);
        availableColArgs.putAll(this.availableColArgs);

        JoinExpressionExtractor visitor = new JoinExpressionExtractor(this.tableCtx, availableColArgs);
        reference.getReferences().forEach(ref -> {
            joinExpressions.addAll(ref.accept(visitor));
        });
        return joinExpressions;
    }

    @Override
    public Set<String> visitJoinReference(JoinReference reference) {

        JoinPath joinPath = reference.getPath();
        List<PathElement> pathElements = joinPath.getPathElements();

        TableContext currentCtx = this.tableCtx;

        for (int i = 0; i < pathElements.size() - 1; i++) {

            PathElement pathElement = pathElements.get(i);
            Type<?> joinClass = pathElement.getFieldType();
            String joinFieldName = pathElement.getFieldName();

            SQLJoin sqlJoin = currentCtx.getQueryable().getJoin(joinFieldName);

            TableContext joinCtx;
            String onClause;
            JoinType joinType;

            if (sqlJoin != null) {
                joinType = sqlJoin.getJoinType();
                joinCtx = (TableContext) currentCtx.get(joinFieldName);

                if (joinType.equals(JoinType.CROSS)) {
                    onClause = EMPTY;
                } else {
                    /**
                     * Resolve handlebars with in Join expression with available column arguments.
                     * Column name can be blank here, as column name is required only for extracting default arguments,
                     *  which are already added to availableColArgs.
                     */
                    onClause = ON + currentCtx.resolveHandlebars(EMPTY,
                                                                 sqlJoin.getJoinExpression(),
                                                                 this.availableColArgs);
                }
            } else {
                joinType = JoinType.LEFT;
                joinCtx = TableContext.builder()
                                .queryable(metaDataStore.getTable(joinClass))
                                .alias(appendAlias(currentCtx.getAlias(), joinFieldName))
                                .metaDataStore(currentCtx.getMetaDataStore())
                                .build();

                onClause = ON + String.format("%s.%s = %s.%s",
                                currentCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(pathElement.getType(), joinFieldName),
                                joinCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(joinClass, dictionary.getIdFieldName(joinClass)));
            }

            String joinAlias = applyQuotes(joinCtx.getAlias(), currentCtx.getQueryable().getDialect());
            String joinKeyword = currentCtx.getQueryable().getDialect().getJoinKeyword(joinType);
            String joinSource = constructTableOrSubselect(joinCtx, joinClass);

            String fullExpression = String.format("%s %s AS %s %s", joinKeyword, joinSource, joinAlias, onClause);
            joinExpressions.add(fullExpression);

            /**
             * If this `for` loop runs more than once, context should be switched to join context.
             * Column arguments will remain same in this scenario, as there will not be any logical column in between,
             *  so there are no new default arguments.
             */
            currentCtx = joinCtx;
        }

        JoinExpressionExtractor visitor = new JoinExpressionExtractor(currentCtx, this.availableColArgs);
        joinExpressions.addAll(reference.getReference().accept(visitor));
        return joinExpressions;
    }

    /**
     * Get the SELECT SQL or tableName for given entity.
     * @param tableCtx {@link TableContext} for resolving table args in query.
     * @param cls Entity class.
     * @return resolved tableName or sql in Subselect/FromSubquery.
     */
    private String constructTableOrSubselect(TableContext tableCtx, Type<?> cls) {

        if (hasSql(cls)) {
            // Resolve any table arguments with in FromSubquery or Subselect
            String selectSql = tableCtx.resolveHandlebars(EMPTY,
                                                          resolveTableOrSubselect(dictionary, cls),
                                                          Collections.emptyMap());
            return OPEN_BRACKET + selectSql + CLOSE_BRACKET;
        }

        return applyQuotes(resolveTableOrSubselect(dictionary, cls), tableCtx.getQueryable().getDialect());
    }
}
