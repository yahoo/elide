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
import com.yahoo.elide.datastores.aggregation.metadata.Context;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
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

    private final Context context;
    private final ColumnProjection column;

    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;

    public JoinExpressionExtractor(Context context, ColumnProjection column) {
        this.context = context;
        this.column = column;
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
         * Creating new visitor with correct ColumnProjection, so that default arguments can be extracted correctly.
         */
        JoinExpressionExtractor visitor = new JoinExpressionExtractor(this.context, reference.getColumn());
        reference.getReferences().forEach(ref -> {
            joinExpressions.addAll(ref.accept(visitor));
        });
        return joinExpressions;
    }

    @Override
    public Set<String> visitJoinReference(JoinReference reference) {

        JoinPath joinPath = reference.getPath();
        List<PathElement> pathElements = joinPath.getPathElements();

        Context currentCtx = this.context;
        String columnName = this.column.getName();

        for (int i = 0; i < pathElements.size() - 1; i++) {

            PathElement pathElement = pathElements.get(i);
            Type<?> joinClass = pathElement.getFieldType();
            String joinFieldName = pathElement.getFieldName();

            SQLJoin sqlJoin = currentCtx.getQueryable().getJoin(joinFieldName);

            Context joinCtx;
            String onClause;
            JoinType joinType;

            if (sqlJoin != null) {
                joinType = sqlJoin.getJoinType();
                joinCtx = (Context) currentCtx.get(joinFieldName);

                if (joinType.equals(JoinType.CROSS)) {
                    onClause = EMPTY;
                } else {
                    onClause = ON + currentCtx.resolveHandlebars(columnName, sqlJoin.getJoinExpression());
                }
            } else {
                joinType = JoinType.LEFT;
                joinCtx = Context.builder()
                                .queryable(metaDataStore.getTable(joinClass))
                                .alias(appendAlias(currentCtx.getAlias(), joinFieldName))
                                .metaDataStore(currentCtx.getMetaDataStore())
                                .queriedColArgs(currentCtx.getQueriedColArgs())
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
             * Column name can be blank here, as column name is required only for extracting default arguments, As there
             *  will not be any logical column in between, so no new default arguments.
             */
            currentCtx = joinCtx;
            columnName = EMPTY;
        }

        // If reference within current join reference is of type PhysicalReference, then below visitor doesn't matter.
        // If it is of type LogicalReference, then visitLogicalReference method will recreate visitor with correct
        // value of ColumnProjection.
        JoinExpressionExtractor visitor = new JoinExpressionExtractor(currentCtx, null);
        joinExpressions.addAll(reference.getReference().accept(visitor));
        return joinExpressions;
    }

    /**
     * Get the SELECT SQL or tableName for given entity.
     * @param context {@link Context} for resolving table args in query.
     * @param cls Entity class.
     * @return resolved tableName or sql in Subselect/FromSubquery.
     */
    private String constructTableOrSubselect(Context context, Type<?> cls) {

        if (hasSql(cls)) {
            // Resolve any table arguments with in FromSubquery or Subselect
            String selectSql = context.resolveHandlebars(EMPTY, resolveTableOrSubselect(dictionary, cls));
            return OPEN_BRACKET + selectSql + CLOSE_BRACKET;
        }

        return applyQuotes(resolveTableOrSubselect(dictionary, cls), context.getQueryable().getDialect());
    }
}
