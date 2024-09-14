/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jpql.filter.FilterTranslator;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.Session;
import com.yahoo.elide.datastores.jpql.porting.SingleResultQuery;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Constructs a HQL query to fetch a root collection.
 */
public class RootCollectionFetchQueryBuilder extends AbstractHQLQueryBuilder {

    public RootCollectionFetchQueryBuilder(EntityProjection entityProjection,
                                           EntityDictionary dictionary,
                                           Session session,
                                           CursorEncoder cursorEncoder) {
        super(entityProjection, dictionary, session, cursorEncoder);
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
        String keysetPaginationClause = getKeysetPaginationClause(entityProjection, dictionary, entityAlias);

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
            if (!"".equals(keysetPaginationClause)) {
                keysetPaginationClause = " AND " + keysetPaginationClause;
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
                            + keysetPaginationClause
                            + SPACE
                            + getSortClause(entityProjection.getSorting())
            );

            //Fill in the query parameters
            supplyFilterQueryParameters(query, predicates);
            if (!"".equals(keysetPaginationClause)) {
                supplyKeysetPaginationQueryParameters(query, entityProjection, dictionary);
            }
        } else {
            if (!"".equals(keysetPaginationClause)) {
                keysetPaginationClause = WHERE + keysetPaginationClause;
            }
            query = session.createQuery(SELECT
                    + entityAlias
                    + FROM
                    + entityName
                    + AS
                    + entityAlias
                    + SPACE
                    + getJoinClauseFromSort(entityProjection.getSorting())
                    + extractToOneMergeJoins(entityClass, entityAlias)
                    + keysetPaginationClause
                    + SPACE
                    + getSortClause(entityProjection.getSorting()));
            if (!"".equals(keysetPaginationClause)) {
                supplyKeysetPaginationQueryParameters(query, entityProjection, dictionary);
            }
        }

        addPaginationToQuery(query);
        return query;
    }
}
