/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.hasSql;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.resolveTableOrSubselect;
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
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;

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

    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;

    public JoinExpressionExtractor(ColumnContext context) {
        this.context = context;
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
                        .build();

        JoinExpressionExtractor visitor = new JoinExpressionExtractor(newCtx);
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
                joinCtx = ColumnContext.builder()
                                .queryable(metaDataStore.getTable(joinClass))
                                .alias(appendAlias(currentCtx.getAlias(), joinFieldName))
                                .metaDataStore(currentCtx.getMetaDataStore())
                                .column(currentCtx.getColumn())
                                .build();

                onClause = ON + String.format("%s.%s = %s.%s",
                                currentCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(pathElement.getType(), joinFieldName),
                                joinCtx.getAlias(),
                                dictionary.getAnnotatedColumnName(joinClass, dictionary.getIdFieldName(joinClass)));
            }

            String joinAlias = applyQuotes(joinCtx.getAlias(), currentCtx.getQueryable().getDialect());
            String joinKeyword = currentCtx.getQueryable().getDialect().getJoinKeyword(joinType);
            String joinSource = constructTableOrSubselect(joinCtx.getQueryable(), joinClass);

            String fullExpression = String.format("%s %s AS %s %s", joinKeyword, joinSource, joinAlias, onClause);
            joinExpressions.add(fullExpression);

            /**
             * If this `for` loop runs more than once, context should be switched to join context.
             */
            currentCtx = joinCtx;
        }

        // If reference within current join reference is of type PhysicalReference, then below visitor doesn't matter.
        // If it is of type LogicalReference, then visitLogicalReference method will recreate visitor with correct
        // value of ColumnProjection in context.
        JoinExpressionExtractor visitor = new JoinExpressionExtractor(currentCtx);
        joinExpressions.addAll(reference.getReference().accept(visitor));
        return joinExpressions;
    }

    /**
     * Get the SELECT SQL or tableName for given entity.
     * @param queryable Queryable.
     * @param cls Entity class.
     * @return resolved tableName or sql in Subselect/FromSubquery.
     */
    private String constructTableOrSubselect(Queryable queryable, Type<?> cls) {

        if (hasSql(cls)) {
            // Resolve any table arguments with in FromSubquery or Subselect
            TableContext context = TableContext.tableContextBuilder().queryable(queryable).build();
            String selectSql = context.resolve(resolveTableOrSubselect(dictionary, cls));
            return OPEN_BRACKET + selectSql + CLOSE_BRACKET;
        }

        return applyQuotes(resolveTableOrSubselect(dictionary, cls), queryable.getDialect());
    }
}
