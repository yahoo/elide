/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.ExpressionScopingVisitor;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Constructs a HQL query to fetch the size of a hibernate collection proxy.
 */
public class SubCollectionPageTotalsQueryBuilder extends AbstractHQLQueryBuilder {

    private final Relationship relationship;

    public SubCollectionPageTotalsQueryBuilder(Relationship relationship,
                                               EntityDictionary dictionary,
                                               Session session) {
        super(dictionary, session);
        this.relationship = relationship;
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
     * Constructs a query that returns the count of the members of a relationship.
     *
     * For a relationship like author#3.books, constructs a query like:
     *
     * SELECT COUNT(DISTINCT Author_books)
     * FROM Author AS Author JOIN Author.books AS Author_books
     * WHERE Author.id = :author_books_id;
     *
     * Rather than query relationship directly (FROM Book), this query starts at the relationship
     * owner to support scenarios where there is no inverse relationship from the relationship back to
     * the owner.
     *
     * @return the constructed query
     */
    @Override
    public Query build() {
        Class<?> parentType = dictionary.lookupEntityClass(relationship.getParentType());
        Class<?> idType = dictionary.getIdType(parentType);
        Object idVal = CoerceUtil.coerce(dictionary.getId(relationship.getParent()), idType);
        String idField = dictionary.getIdFieldName(parentType);

        //Construct a predicate that selects an individual element of the relationship's parent (Author.id = 3).
        FilterPredicate idExpression = new InPredicate(new PathElement(parentType, idType, idField), idVal);

        Collection<FilterPredicate> predicates = new ArrayList<>();
        String joinClause = "";
        String filterClause = "";

        String relationshipName = relationship.getRelationshipName();

        //Relationship alias is Author_books
        String parentAlias = FilterPredicate.getTypeAlias(parentType);
        String relationshipAlias = parentAlias + UNDERSCORE + relationshipName;

        if (filterExpression.isPresent()) {
            // Copy and scope the filter expression for the join clause
            ExpressionScopingVisitor visitor = new ExpressionScopingVisitor(
                    new PathElement(parentType, relationship.getChildType(), relationship.getRelationshipName()));
            FilterExpression scoped = filterExpression
                    .map(fe -> fe.accept(visitor))
                    .orElseThrow(() -> new IllegalStateException("Filter expression cloned to null"));

            //For each filter predicate, prepend the predicate with the parent:
            //books.title = 'Foobar' becomes author.books.title = 'Foobar'
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor(new ArrayList<>());

            predicates = scoped.accept(extractor);
            predicates.add(idExpression);

            //Join together the provided filter expression with the expression which selects the collection owner.
            FilterExpression joinedExpression = new AndFilterExpression(scoped, idExpression);

            //Build the JOIN clause from the filter predicate
            joinClause = getJoinClauseFromFilters(joinedExpression);

            //Build the WHERE clause
            filterClause = new HQLFilterOperation().apply(joinedExpression, USE_ALIAS);
        } else {

            //If there is no filter, we still need to explicitly JOIN book and authors.
            joinClause = JOIN
                    + parentAlias
                    + PERIOD + relationshipName
                    + SPACE
                    + relationshipAlias
                    + SPACE;

            filterClause = new HQLFilterOperation().apply(idExpression, USE_ALIAS);
            predicates.add(idExpression);
        }

        Query query = session.createQuery(
                "SELECT COUNT(DISTINCT "
                        + relationshipAlias
                        + ") "
                        + FROM
                        + parentType.getCanonicalName()
                        + AS
                        + parentAlias
                        + SPACE
                        + joinClause
                        + SPACE
                        + filterClause);

        //Fill in the query parameters
        supplyFilterQueryParameters(query, predicates);
        return query;
    }
}
