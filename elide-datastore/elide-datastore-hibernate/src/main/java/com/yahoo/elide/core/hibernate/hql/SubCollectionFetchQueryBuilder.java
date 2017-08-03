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
 * Constructs a HQL query to fetch a hibernate collection proxy.
 */
public class SubCollectionFetchQueryBuilder extends AbstractHQLQueryBuilder {

    private final Relationship relationship;

    public SubCollectionFetchQueryBuilder(Relationship relationship,
                                          EntityDictionary dictionary,
                                          Session session) {
        super(dictionary, session);
        this.relationship = relationship;
    }

    /**
     * Constructs a query that returns the members of a relationship.
     *
     * @return the constructed query or null if the collection proxy does not require any
     * sorting, pagination, or filtering.
     */
    @Override
    public Query build() {

        if (!filterExpression.isPresent() && !pagination.isPresent() && !sorting.isPresent()) {
            return null;
        }

        Query query;
        if (filterExpression.isPresent()) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = filterExpression.get().accept(extractor);
            String filterClause = new HQLFilterOperation().apply(filterExpression.get(), false);

            // We don't prefix with aliases because we are not joining across toMany relationships.
            query = session.createFilter(relationship.getChildren(),
                filterClause + SPACE + getSortClause(sorting, relationship.getChildType(), false));

            supplyFilterQueryParameters(query, predicates);
        } else {
            query = session.createFilter(relationship.getChildren(),

                //The root collection doesn't need prefix for order by clause.
                getSortClause(sorting, relationship.getChildType(), false));
        }


        addPaginationToQuery(query);
        return query;
    }
}
