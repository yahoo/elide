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
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpql.filter.FilterTranslator;
import com.paiondata.elide.datastores.jpql.porting.Query;
import com.paiondata.elide.datastores.jpql.porting.Session;
import com.paiondata.elide.datastores.jpql.porting.SingleResultQuery;

import java.util.Collection;
import java.util.function.Supplier;

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
        Type<?> entityClass = this.entityProjection.getType();
        String entityName = entityClass.getCanonicalName();
        String entityAlias = getTypeAlias(entityClass);

        Query query;
        FilterExpression filterExpression = entityProjection.getFilterExpression();
        if (filterExpression != null) {
            //Build the JOIN clause
            String joinClause = getJoinClauseFromFilters(filterExpression)
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(entityClass, entityAlias);

            //Build the WHERE clause
            String filterClause = WHERE + new FilterTranslator(dictionary).apply(filterExpression, USE_ALIAS);

            if (joinClause.isEmpty() && filterExpression instanceof InPredicate inPredicate
                    && entityProjection.getSorting() == null
                    && inPredicate.getField().equals(dictionary.getIdFieldName(entityClass))
                    && inPredicate.getValues().size() == 1) {
                // This is used to utilize the second-level cache
                // Otherwise for JPQL queries the second-level cache is used only if the query result cache is used
                // The query result cache is only used if
                // - Setting hibernate.cache.use_query_cache is true
                // - Query hint org.hibernate.cacheable is true
                // - scroll is not used but list
                // @see org.hibernate.sql.results.spi.ScrollableResultsConsumer#canResultsBeCached
                String queryText = SELECT + entityAlias + FROM + entityName + AS + entityAlias + filterClause;
                Supplier<Object> result = () -> session.find(queryText, entityClass.getUnderlyingClass().get(),
                        inPredicate.getValues().get(0));
                return new SingleResultQuery(result);
            }

            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            Collection<FilterPredicate> predicates = filterExpression.accept(extractor);

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
                    + SPACE
                    + getSortClause(entityProjection.getSorting()));
        }

        addPaginationToQuery(query);
        return query;
    }
}
