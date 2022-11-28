/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.applyQuotes;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.mergedArgumentMap;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.hasSql;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.resolveTableOrSubselect;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.ColumnContext;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.TableContext;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides Join Expressions for all the {@link JoinReference} in a given reference tree.
 */
public class JoinExpressionExtractor implements ReferenceVisitor<Set<String>> {

    private static final String ON = "ON ";
    private static final String OPEN_BRACKET = "(";
    private static final String CLOSE_BRACKET = ")";

    private final Set<String> joinExpressions = new LinkedHashSet<>();

    private final ColumnContext context;
    private final Query clientQuery;

    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;

    public JoinExpressionExtractor(ColumnContext context, Query clientQuery) {
        this.context = context;
        this.clientQuery = clientQuery;
        this.metaDataStore = context.getMetaDataStore();
        this.dictionary = context.getMetaDataStore().getMetadataDictionary();
    }

    @Override
    public Set<String> visitPhysicalReference(PhysicalReference reference) {
        return joinExpressions;
    }

    @Override
    public Set<String> visitLogicalReference(LogicalReference reference) {

        /**
         * For the scenario: col1:{{col2}}, col2:{{col3}}, col3:{{join.col1}}
         * Creating new visitor with new ColumnProjection.
         */
        ColumnProjection newColumn = reference.getColumn();

        ColumnContext newCtx = ColumnContext.builder()
                        .queryable(this.context.getQueryable())
                        .alias(this.context.getAlias())
                        .metaDataStore(this.context.getMetaDataStore())
                        .column(newColumn)
                        .tableArguments(this.context.getTableArguments())
                        .build();

        JoinExpressionExtractor visitor = new JoinExpressionExtractor(newCtx, clientQuery);
        reference.getReferences().forEach(ref -> {
            joinExpressions.addAll(ref.accept(visitor));
        });
        return joinExpressions;
    }

    @Override
    public Set<String> visitJoinReference(JoinReference reference) {

        JoinPath joinPath = reference.getPath();
        List<PathElement> pathElements = joinPath.getPathElements();

        ColumnContext currentCtx = this.context;

        for (int i = 0; i < pathElements.size() - 1; i++) {

            PathElement pathElement = pathElements.get(i);
            Type<?> joinClass = pathElement.getFieldType();
            String joinFieldName = pathElement.getFieldName();

            SQLJoin sqlJoin = currentCtx.getQueryable().getJoin(joinFieldName);

            ColumnContext joinCtx;
            String onClause;
            JoinType joinType;
            String fullExpression;

            if (sqlJoin != null) {
                joinType = sqlJoin.getJoinType();
                joinCtx = (ColumnContext) currentCtx.get(joinFieldName);

                if (joinType.equals(JoinType.CROSS)) {
                    onClause = EMPTY;
                } else {
                    onClause = ON + currentCtx.resolve(sqlJoin.getJoinExpression());
                }
            } else {
                joinType = JoinType.LEFT;
                SQLTable table = metaDataStore.getTable(joinClass);
                joinCtx = ColumnContext.builder()
                                .queryable(table)
                                .alias(appendAlias(currentCtx.getAlias(), joinFieldName))
                                .metaDataStore(currentCtx.getMetaDataStore())
                                .column(currentCtx.getColumn())
                                .tableArguments(mergedArgumentMap(table.getArguments(),
                                                                  currentCtx.getTableArguments()))
                                .build();

                onClause = ON + String.format("%s.%s = %s.%s",
                                currentCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(pathElement.getType(), joinFieldName),
                                joinCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(joinClass, dictionary.getIdFieldName(joinClass)));
            }

            SQLDialect sqlDialect = currentCtx.getQueryable().getDialect();
            String joinAlias = applyQuotes(joinCtx.getAlias(), sqlDialect);
            String joinKeyword = currentCtx.getQueryable().getDialect().getJoinKeyword(joinType);
            String joinSource = constructTableOrSubselect(joinCtx, joinClass);

            if (sqlDialect.useASBeforeTableAlias()) {
                fullExpression = String.format("%s %s AS %s %s", joinKeyword, joinSource, joinAlias, onClause);
            } else {
                fullExpression = String.format("%s %s %s %s", joinKeyword, joinSource, joinAlias, onClause);
            }
            joinExpressions.add(fullExpression);

            /**
             * If this `for` loop runs more than once, context should be switched to join context.
             */
            currentCtx = joinCtx;
        }

        // If reference within current join reference is of type PhysicalReference, then below visitor doesn't matter.
        // If it is of type LogicalReference, then visitLogicalReference method will recreate visitor with correct
        // value of ColumnProjection in context.
        JoinExpressionExtractor visitor = new JoinExpressionExtractor(currentCtx, clientQuery);
        joinExpressions.addAll(reference.getReference().accept(visitor));
        return joinExpressions;
    }

    /**
     * Get the SELECT SQL or tableName for given entity.
     * @param columnCtx {@link ColumnContext}.
     * @param cls Entity class.
     * @return resolved tableName or sql in Subselect/FromSubquery.
     */
    private String constructTableOrSubselect(ColumnContext columnCtx, Type<?> cls) {

        if (hasSql(cls)) {
            // Resolve any table arguments with in FromSubquery or Subselect
            TableContext context = TableContext.builder().tableArguments(columnCtx.getTableArguments()).build();
            String selectSql = context.resolve(resolveTableOrSubselect(dictionary, cls, clientQuery));
            return OPEN_BRACKET + selectSql + CLOSE_BRACKET;
        }

        return applyQuotes(resolveTableOrSubselect(dictionary, cls, clientQuery),
                columnCtx.getQueryable().getDialect());
    }

    @Override
    public Set<String> visitColumnArgReference(ColumnArgReference reference) {
        return emptySet();
    }

    @Override
    public Set<String> visitTableArgReference(TableArgReference reference) {
        return emptySet();
    }
}
