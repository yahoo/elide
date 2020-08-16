/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;

import java.util.Collection;
import java.util.function.Function;

/**
 * Constructs a HQL query to fetch a hibernate collection proxy.
 */
public class SubCollectionFetchQueryBuilder extends AbstractHQLQueryBuilder {

    private final Relationship relationship;

    public SubCollectionFetchQueryBuilder(Relationship relationship,
                                          EntityDictionary dictionary,
                                          Session session) {
        super(relationship.getRelationship().getProjection(), dictionary, session);
        this.relationship = relationship;
    }

    @Override
    protected String extractToOneMergeJoins(Class<?> entityClass, String alias) {
        Function<String, Boolean> shouldSkip = (relationshipName) -> {
            String inverseRelationName = dictionary.getRelationInverse(entityClass, relationshipName);
            if (inverseRelationName.isEmpty()) {
                return false;
            }

            Class<?> relationshipClass = dictionary.getParameterizedType(entityClass, relationshipName);

            //We don't need (or want) to fetch join the parent object.
            return relationshipClass.equals(relationship.getParentType())
                    && inverseRelationName.equals(relationship.getRelationshipName());
        };

        return extractToOneMergeJoins(entityClass, alias, shouldSkip);
    }

    /**
     * Constructs a query that returns the members of a relationship.
     *
     * @return the constructed query or null if the collection proxy does not require any
     * sorting, pagination, or filtering.
     */
    @Override
    public Query build() {

        if (entityProjection.getFilterExpression() == null && entityProjection.getPagination() == null
                && (entityProjection.getSorting() == null || entityProjection.getSorting().isDefaultInstance())) {
            return null;
        }

        String childAlias = getTypeAlias(relationship.getChildType());
        String parentAlias = getTypeAlias(relationship.getParentType()) + "__fetch";
        String parentName = relationship.getParentType().getCanonicalName();
        String relationshipName = relationship.getRelationshipName();

        FilterExpression filterExpression = entityProjection.getFilterExpression();
        Query query;
        if (filterExpression != null) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = filterExpression.accept(extractor);
            String filterClause = new FilterTranslator().apply(filterExpression, USE_ALIAS);

            String joinClause =  getJoinClauseFromFilters(filterExpression)
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(relationship.getChildType(), childAlias);

            //SELECT parent_children from Parent parent JOIN parent.children parent_children
            query = session.createQuery(SELECT
                    + childAlias
                    + FROM
                    + parentName + SPACE + parentAlias
                    + JOIN
                    + parentAlias + PERIOD + relationshipName + SPACE + childAlias
                    + joinClause
                    + WHERE
                    + filterClause
                    + " AND " + parentAlias + "=:" + parentAlias
                    + SPACE
                    + getSortClause(entityProjection.getSorting())
            );

            supplyFilterQueryParameters(query, predicates);
        } else {
            query = session.createQuery(SELECT
                    + childAlias
                    + FROM
                    + parentName + SPACE + parentAlias
                    + JOIN
                    + parentAlias + PERIOD + relationshipName + SPACE + childAlias
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(relationship.getChildType(), childAlias)
                    + " WHERE " + parentAlias + "=:" + parentAlias
                    + getSortClause(entityProjection.getSorting())
            );
        }

        query.setParameter(parentAlias, relationship.getParent());

        addPaginationToQuery(query);
        return query;
    }
}
