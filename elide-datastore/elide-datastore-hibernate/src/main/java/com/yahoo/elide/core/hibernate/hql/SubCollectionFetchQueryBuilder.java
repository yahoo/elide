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

        if (!filterExpression.isPresent() && !pagination.isPresent()
                && (!sorting.isPresent() || sorting.get().isDefaultInstance())) {
            return null;
        }

        String childAlias = FilterPredicate.getTypeAlias(relationship.getChildType());
        String parentAlias = FilterPredicate.getTypeAlias(relationship.getParentType()) + "__fetch";
        String parentName = relationship.getParentType().getCanonicalName();
        String relationshipName = relationship.getRelationshipName();

        Query query = filterExpression.map(fe -> {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = fe.accept(extractor);
            String filterClause = new HQLFilterOperation().apply(fe, USE_ALIAS);

            String joinClause =  getJoinClauseFromFilters(filterExpression.get())
                    + extractToOneMergeJoins(relationship.getChildType(), childAlias);

            //SELECT parent_children from Parent parent JOIN parent.children parent_children
            Query q = session.createQuery(SELECT
                            + childAlias
                            + FROM
                            + parentName + SPACE + parentAlias
                            + JOIN
                            + parentAlias + PERIOD + relationshipName + SPACE + childAlias
                            + joinClause
                            + SPACE
                            + filterClause
                            + " AND " + parentAlias + "=:" + parentAlias
                            + SPACE
                            + getSortClause(sorting, relationship.getChildType(), USE_ALIAS)
            );

            supplyFilterQueryParameters(q, predicates);
            return q;
        }).orElse(session.createQuery(SELECT
                            + childAlias
                            + FROM
                            + parentName + SPACE + parentAlias
                            + JOIN
                            + parentAlias + PERIOD + relationshipName + SPACE + childAlias
                            + extractToOneMergeJoins(relationship.getChildType(), childAlias)
                            + " WHERE " + parentAlias + "=:" + parentAlias
                            + getSortClause(sorting, relationship.getChildType(), USE_ALIAS)
        ));

        query.setParameter(parentAlias, relationship.getParent());

        addPaginationToQuery(query);
        return query;
    }
}
