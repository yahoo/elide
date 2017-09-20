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
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * Constructs a HQL query to fetch the size of a root collection.
 */
public class RootCollectionPageTotalsQueryBuilder extends AbstractHQLQueryBuilder {

    private Class<?> entityClass;

    public RootCollectionPageTotalsQueryBuilder(Class<?> entityClass,
                                                EntityDictionary dictionary,
                                                Session session) {
        super(dictionary, session);
        this.entityClass = dictionary.lookupEntityClass(entityClass);
    }

    @Override
    public AbstractHQLQueryBuilder withPossiblePagination(Optional<Pagination> ignored) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractHQLQueryBuilder withPossibleSorting(Optional<Sorting> ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs a query that returns the count of a root collection.
     *
     * Constructs a query like:
     *
     * SELECT COUNT(DISTINCT Author)
     * FROM Author AS Author
     *
     * @return the constructed query
     */
    @Override
    public Query build() {
        String entityName = entityClass.getCanonicalName();
        String entityAlias = FilterPredicate.getTypeAlias(entityClass);

        Collection<FilterPredicate> predicates;

        String filterClause;
        String joinClause;

        if (filterExpression.isPresent()) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            predicates = filterExpression.get().accept(extractor);

            //Build the WHERE clause
            filterClause = new HQLFilterOperation().apply(filterExpression.get(), USE_ALIAS);

            //Build the JOIN clause
            joinClause =  getJoinClauseFromFilters(filterExpression.get());

        } else {
            predicates = new HashSet();
            filterClause = "";
            joinClause = "";
        }

        Query query = session.createQuery("SELECT COUNT(DISTINCT "
                + entityAlias
                + ") "
                + FROM
                + entityName
                + AS
                + entityAlias
                + SPACE
                + joinClause
                + SPACE
                + filterClause
        );
        supplyFilterQueryParameters(query, predicates);
        return query;
    }
}
