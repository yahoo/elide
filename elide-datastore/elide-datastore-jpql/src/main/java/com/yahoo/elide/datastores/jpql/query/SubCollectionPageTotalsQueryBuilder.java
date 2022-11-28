/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.ExpressionScopingVisitor;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.jpql.filter.FilterTranslator;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.Session;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Constructs a HQL query to fetch the size of a hibernate collection proxy.
 */
public class SubCollectionPageTotalsQueryBuilder extends AbstractHQLQueryBuilder {

    private final Relationship relationship;

    public SubCollectionPageTotalsQueryBuilder(Relationship relationship,
                                               EntityDictionary dictionary,
                                               Session session) {
        super(relationship.getRelationship().getProjection(), dictionary, session);
        this.relationship = relationship;
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
        Type<?> parentType = dictionary.lookupEntityClass(relationship.getParentType());
        Type<?> idType = dictionary.getIdType(parentType);
        Object idVal = CoerceUtil.coerce(dictionary.getId(relationship.getParent()), idType);
        String idField = dictionary.getIdFieldName(parentType);

        //Construct a predicate that selects an individual element of the relationship's parent (Author.id = 3).
        FilterPredicate idExpression = new InPredicate(new PathElement(parentType, idType, idField), idVal);

        Collection<FilterPredicate> predicates = new ArrayList<>();
        String joinClause = "";
        String filterClause = "";

        String relationshipName = relationship.getRelationshipName();

        //Relationship alias is Author_books
        String parentAlias = getTypeAlias(parentType);
        String relationshipAlias = appendAlias(parentAlias, relationshipName);

        FilterExpression filterExpression = entityProjection.getFilterExpression();
        if (filterExpression != null) {
            // Copy and scope the filter expression for the join clause
            ExpressionScopingVisitor visitor = new ExpressionScopingVisitor(
                    new PathElement(parentType, relationship.getChildType(), relationship.getRelationshipName()));

            FilterExpression scoped = filterExpression.accept(visitor);

            //For each filter predicate, prepend the predicate with the parent:
            //books.title = 'Foobar' becomes author.books.title = 'Foobar'
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor(new ArrayList<>());

            predicates = scoped.accept(extractor);
            predicates.add(idExpression);

            //Join together the provided filter expression with the expression which selects the collection owner.
            FilterExpression joinedExpression = new AndFilterExpression(scoped, idExpression);

            //Build the JOIN clause from the filter predicate
            joinClause = getJoinClauseFromFilters(joinedExpression, true);

            //Build the WHERE clause
            filterClause = new FilterTranslator(dictionary).apply(joinedExpression, USE_ALIAS);
        } else {

            //If there is no filter, we still need to explicitly JOIN book and authors.
            joinClause = JOIN
                    + parentAlias
                    + PERIOD + relationshipName
                    + SPACE
                    + relationshipAlias
                    + SPACE;

            filterClause = new FilterTranslator(dictionary).apply(idExpression, USE_ALIAS);
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
                        + WHERE
                        + filterClause);

        //Fill in the query parameters
        supplyFilterQueryParameters(query, predicates);
        return query;
    }
}
