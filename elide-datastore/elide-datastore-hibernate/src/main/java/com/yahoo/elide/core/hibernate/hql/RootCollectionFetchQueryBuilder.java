/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;

import java.util.Collection;

/**
 * Constructs a HQL query to fetch a root collection.
 */
public class RootCollectionFetchQueryBuilder extends AbstractHQLQueryBuilder {

    private Class<?> entityClass;

    public RootCollectionFetchQueryBuilder(Class<?> entityClass,
                                           EntityDictionary dictionary,
                                           Session session) {
        super(dictionary, session);
        this.entityClass = dictionary.lookupEntityClass(entityClass);
    }

    /**
     * Constructs a query that fetches a root collection.
     *
     * @return the constructed query
     */
    @Override
    public Query build() {
        String entityName = entityClass.getCanonicalName();
        String entityAlias = FilterPredicate.getTypeAlias(entityClass);

        Query query;
        if (filterExpression.isPresent()) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = filterExpression.get().accept(extractor);

            //Build the WHERE clause
            String filterClause = new HQLFilterOperation().apply(filterExpression.get(), USE_ALIAS);

            //Build the JOIN clause
            String joinClause =  getJoinClauseFromFilters(filterExpression.get())
                    + extractToOneMergeJoins(entityClass, entityAlias);

            query = session.createQuery(
                    SELECT
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
                        + getSortClause(sorting, entityClass, USE_ALIAS)
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
                    + extractToOneMergeJoins(entityClass, entityAlias)
                    + SPACE
                    + getSortClause(sorting, entityClass, USE_ALIAS));
        }

        addPaginationToQuery(query);
        return query;
    }
}
