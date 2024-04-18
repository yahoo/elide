/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.query;

import static com.paiondata.elide.core.utils.TypeHelper.getTypeAlias;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpql.filter.FilterTranslator;
import com.paiondata.elide.datastores.jpql.porting.Query;
import com.paiondata.elide.datastores.jpql.porting.Session;

import java.util.Collection;
import java.util.function.Predicate;

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
    protected String extractToOneMergeJoins(Type<?> entityClass, String alias) {
        Predicate<String> shouldSkip = (relationshipName) -> {
            String inverseRelationName = dictionary.getRelationInverse(entityClass, relationshipName);
            if (inverseRelationName.isEmpty()) {
                return false;
            }

            Type<?> relationshipClass = dictionary.getParameterizedType(entityClass, relationshipName);

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
            String filterClause = new FilterTranslator(dictionary).apply(filterExpression, USE_ALIAS);

            String joinClause =  getJoinClauseFromFilters(filterExpression)
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(relationship.getChildType(), childAlias);

            boolean requiresDistinct = containsOneToMany(filterExpression);

            boolean sortOverRelationship = entityProjection.getSorting() != null
                    && entityProjection.getSorting().getSortingPaths().keySet()
                    .stream().anyMatch(path ->
                            path.getPathElements()
                                    .stream()
                                    .anyMatch(element ->
                                            dictionary.isRelation(element.getType(), element.getFieldName())));

            if (requiresDistinct && sortOverRelationship) {
                //SQL does not support distinct and order by on columns which are not selected
                throw new InvalidValueException("Combination of sorting over relationship and"
                        + " filtering over toMany relationships unsupported");
            }
            //SELECT parent_children from Parent parent JOIN parent.children parent_children
            query = session.createQuery(SELECT
                    + (requiresDistinct ? DISTINCT : "")
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
