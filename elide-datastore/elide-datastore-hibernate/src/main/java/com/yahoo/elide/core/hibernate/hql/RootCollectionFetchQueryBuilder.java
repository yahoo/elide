/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;
import com.yahoo.elide.request.EntityProjection;

import java.util.Collection;

/**
 * Constructs a HQL query to fetch a root collection.
 */
public class RootCollectionFetchQueryBuilder extends AbstractHQLQueryBuilder {

    public RootCollectionFetchQueryBuilder(EntityProjection entityProjection,
                                           EntityDictionary dictionary,
                                           Session session) {
        super(entityProjection, dictionary, session);
    }

    /**
     * Constructs a query that fetches a root collection.
     *
     * @return the constructed query
     */
    @Override
    public Query build() {
        Class<?> entityClass = this.entityProjection.getType();
        String entityName = entityClass.getCanonicalName();
        String entityAlias = getTypeAlias(entityClass);

        Query query;
        FilterExpression filterExpression = entityProjection.getFilterExpression();
        if (filterExpression != null) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = filterExpression.accept(extractor);

            //Build the WHERE clause
            String filterClause = WHERE + new FilterTranslator().apply(filterExpression, USE_ALIAS);

            //Build the JOIN clause
            String joinClause =  getJoinClauseFromFilters(filterExpression)
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(entityClass, entityAlias);

            boolean requiresDistinct = entityProjection.getPagination() != null
                    && containsOneToMany(filterExpression);

            Boolean sortOverRelationship = entityProjection.getSorting() != null
                    && entityProjection.getSorting().getSortingPaths().keySet()
                            .stream().anyMatch(path -> path.getPathElements().size() > 1);
            if (requiresDistinct && sortOverRelationship) {
                //SQL does not support distinct and order by on columns which are not selected
                throw new InvalidValueException("Combination of pagination, sorting over relationship and"
                    + " filtering over toMany relationships unsupported");
            }
            query = session.createQuery(
                    SELECT
                        + (requiresDistinct ? DISTINCT : "")
                        + entityAlias
                        + FROM
                        + entityName
                        + AS
                        + entityAlias
                        + SPACE
                        + joinClause
                        + SPACE
                        + filterClause
                        + SPACE
                        + getSortClause(entityProjection.getSorting())
            );

            //Fill in the query parameters
            supplyFilterQueryParameters(query, predicates);
        } else {
            query = session.createQuery(SELECT
                    + entityAlias
                    + FROM
                    + entityName
                    + AS
                    + entityAlias
                    + SPACE
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(entityClass, entityAlias)
                    + explicitSortJoins(sorting, entityClass)
                    + SPACE
                    + getSortClause(entityProjection.getSorting()));
        }

        addPaginationToQuery(query);
        return query;
    }
}
