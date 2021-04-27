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
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.TableContext;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides Join Expressions for all the {@link JoinReference} in a given reference tree.
 */
public class JoinReferenceExtractor implements ReferenceVisitor<Set<String>> {

    private static final String ON = "ON ";
    private static final String OPEN_BRACKET = "(";
    private static final String CLOSE_BRACKET = ")";

    private final Set<String> joinExpressions;
    private final TableContext tableCtx;
    private final Map<String, Argument> columnArgs;
    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;

    public JoinReferenceExtractor(TableContext tableCtx, Map<String, Argument> columnArgs,
                    Set<String> joinExpressions) {
        this.tableCtx = tableCtx;
        this.joinExpressions = joinExpressions;
        this.columnArgs = columnArgs;
        this.metaDataStore = tableCtx.getMetaDataStore();
        this.dictionary = tableCtx.getMetaDataStore().getMetadataDictionary();
    }

    @Override
    public Set<String> visitPhysicalReference(PhysicalReference reference) {
        return joinExpressions;
    }

    @Override
    public Set<String> visitLogicalReference(LogicalReference reference) {
        reference.getReferences().stream().forEach(ref -> ref.accept(this));
        return joinExpressions;
    }

    @Override
    public Set<String> visitJoinReference(JoinReference reference) {

        JoinPath joinPath = reference.getPath();
        List<PathElement> pathElements = joinPath.getPathElements();

        TableContext currentCtx = tableCtx;

        for (int i = 0; i < pathElements.size() - 1; i++) {

            PathElement pathElement = pathElements.get(i);
            Type<?> joinClass = pathElement.getFieldType();
            String joinFieldName = pathElement.getFieldName();

            SQLJoin sqlJoin = currentCtx.getJoin(joinFieldName);

            TableContext joinCtx;
            String onClause;
            JoinType joinType;

            if (sqlJoin != null) {
                joinType = sqlJoin.getJoinType();
                joinCtx = (TableContext) currentCtx.get(joinFieldName);

                if (joinType.equals(JoinType.CROSS)) {
                    onClause = EMPTY;
                } else {
                    // Resolve handlebars with in Join expression with provided column arguments.
                    onClause = ON + currentCtx.resolveHandlebars(EMPTY,
                                                                 sqlJoin.getJoinExpression(),
                                                                 this.columnArgs);
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

            String joinAlias = applyQuotes(joinCtx.getAlias(), currentCtx.getDialect());
            String joinKeyword = currentCtx.getDialect().getJoinKeyword(joinType);
            String joinSource = constructTableOrSubselect(joinCtx, joinClass);

            String fullExpression = String.format("%s %s AS %s %s", joinKeyword, joinSource, joinAlias, onClause);
            joinExpressions.add(fullExpression);
            currentCtx = joinCtx;
        }

        reference.getReference().accept(new JoinReferenceExtractor(currentCtx, this.columnArgs, joinExpressions));
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

        return applyQuotes(resolveTableOrSubselect(dictionary, cls), tableCtx.getDialect());
    }
}
