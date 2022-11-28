/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.Session;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract class used to construct HQL queries.
 */
public abstract class AbstractHQLQueryBuilder {
    protected final Session session;
    protected final EntityDictionary dictionary;
    protected EntityProjection entityProjection;
    protected static final String SPACE = " ";
    protected static final String PERIOD = ".";
    protected static final String COMMA = ",";
    protected static final String FROM = " FROM ";
    protected static final String JOIN = " JOIN ";
    protected static final String LEFT = " LEFT";
    protected static final String FETCH = " FETCH ";
    protected static final String SELECT = "SELECT ";
    protected static final String AS = " AS ";
    protected static final String DISTINCT = "DISTINCT ";
    protected static final String WHERE = " WHERE ";

    protected static final boolean USE_ALIAS = true;
    protected static final boolean NO_ALIAS = false;
    protected Set<String> alreadyJoined = new HashSet<>();

    /**
     * Represents a relationship between two entities.
     */
    public interface Relationship {

        com.yahoo.elide.core.request.Relationship getRelationship();

        Type<?> getParentType();

        default Type<?> getChildType() {
            return getRelationship().getProjection().getType();
        }

        default String getRelationshipName() {
            return getRelationship().getName();
        }

        Object getParent();
    }

    public AbstractHQLQueryBuilder(EntityProjection entityProjection, EntityDictionary dictionary, Session session) {
        this.session = session;
        this.dictionary = dictionary;
        this.entityProjection = entityProjection;
    }

    public abstract Query build();

    /**
     * Given a collection of filter predicates and a Hibernate query, populates the named parameters in the
     * Hibernate query.
     *
     * @param query The HQL query
     * @param predicates The predicates to extract named parameter values from
     */
    protected void supplyFilterQueryParameters(Query query, Collection<FilterPredicate> predicates) {
        for (FilterPredicate filterPredicate : predicates) {
            if (filterPredicate.getOperator().isParameterized()) {
                boolean shouldEscape = filterPredicate.isMatchingOperator();
                filterPredicate.getParameters().forEach(param ->
                    query.setParameter(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue())
                );
            }
        }
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param filterExpression the filter expression to extract a join clause from
     * @return an HQL join clause
     */
    protected String getJoinClauseFromFilters(FilterExpression filterExpression) {
        return getJoinClauseFromFilters(filterExpression, false);
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param filterExpression the filter expression to extract a join clause from
     * @param skipFetches JOIN but don't FETCH JOIN a relationship.
     * @return an HQL join clause
     */
    protected String getJoinClauseFromFilters(FilterExpression filterExpression, boolean skipFetches) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor(new ArrayList<>());
        Collection<FilterPredicate> predicates = filterExpression.accept(visitor);

        return predicates.stream()
                .map(predicate -> extractJoinClause(predicate.getPath(), skipFetches))
                .collect(Collectors.joining(SPACE));
    }


    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param sorting the sort expression to extract a join clause from
     * @return an HQL join clause
     */
    protected String getJoinClauseFromSort(Sorting sorting) {
        return getJoinClauseFromSort(sorting, false);
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param sorting the sort expression to extract a join clause from
     * @param skipFetches JOIN but don't FETCH JOIN a relationship.
     * @return an HQL join clause
     */
    protected String getJoinClauseFromSort(Sorting sorting, boolean skipFetches) {
        if (sorting != null && !sorting.isDefaultInstance()) {
            Map<Path, Sorting.SortOrder> validSortingRules = sorting.getSortingPaths();
            return validSortingRules.keySet().stream()
                    .map(path -> extractJoinClause(path, skipFetches))
                    .collect(Collectors.joining(SPACE));
        }
        return "";
    }


    /**
     * Modifies the HQL query to add OFFSET and LIMIT.
     * @param query The HQL query object
     */
    protected void addPaginationToQuery(Query query) {
        Pagination pagination = entityProjection.getPagination();
        if (pagination != null) {
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }
    }

    /**
     * Extracts a join clause from a path (if it exists).
     * @param path The path to examine
     * @param skipFetches Don't fetch join
     * @return A HQL string representing the join
     */
    private String extractJoinClause(Path path, boolean skipFetches) {
        StringBuilder joinClause = new StringBuilder();

        String previousAlias = null;

        for (Path.PathElement pathElement : path.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Type<?> typeClass = dictionary.lookupEntityClass(pathElement.getType());
            String typeAlias = getTypeAlias(typeClass);

            // Nothing left to join.
            if (! dictionary.isRelation(pathElement.getType(), fieldName)) {
                return joinClause.toString();
            }

            String alias = previousAlias == null
                    ? appendAlias(typeAlias, fieldName)
                    : appendAlias(previousAlias, fieldName);

            String joinKey;

            //This is the first path element
            if (previousAlias == null) {
                joinKey = typeAlias + PERIOD + fieldName;
            } else {
                joinKey = previousAlias + PERIOD + fieldName;
            }

            String fetch = "";
            RelationshipType type = dictionary.getRelationshipType(pathElement.getType(), fieldName);

            //This is a to-One relationship belonging to the collection being retrieved.
            if (!skipFetches && entityProjection.getIncludedRelationsName().contains(fieldName) && type.isToOne()
                    && !type.isComputed() && previousAlias == null) {
                fetch = "FETCH ";

            }
            String joinFragment = LEFT + JOIN + fetch + joinKey + SPACE + alias + SPACE;

            if (!alreadyJoined.contains(joinKey)) {
                joinClause.append(joinFragment);
                alreadyJoined.add(joinKey);
            }

            previousAlias = alias;
        }

        return joinClause.toString();
    }

    /**
     * Builds a JOIN clause that eagerly fetches to-one relationships that Hibernate needs to hydrate.
     * @param entityClass The entity class that is being queried in the HQL query.
     * @param alias The HQL alias for the entity class.
     * @return The JOIN clause that can be added to the FROM clause.
     */
    protected String extractToOneMergeJoins(Type<?> entityClass, String alias) {
        return extractToOneMergeJoins(entityClass, alias, (unused) -> false);
    }

    protected String extractToOneMergeJoins(Type<?> entityClass, String alias,
                                            Predicate<String> skipRelation) {
        List<String> relationshipNames = dictionary.getRelationships(entityClass);
        StringBuilder joinString = new StringBuilder("");
        for (String relationshipName : relationshipNames) {
            if (!entityProjection.getIncludedRelationsName().contains(relationshipName)) {
                continue;
            }
            RelationshipType type = dictionary.getRelationshipType(entityClass, relationshipName);
            if (type.isToOne() && !type.isComputed()) {
                if (skipRelation.test(relationshipName)) {
                    continue;
                }
                String joinKey = alias + PERIOD + relationshipName;

                if (alreadyJoined.contains(joinKey)) {
                    continue;
                }

                joinString.append(" LEFT JOIN FETCH ");
                joinString.append(joinKey);
                joinString.append(SPACE);
                alreadyJoined.add(joinKey);
            }
        }
        return joinString.toString();
    }

    /**
     * Returns a sorting object into a HQL ORDER BY string.
     * @param sorting The sorting object passed from the client
     * @return The sorting clause
     */
    protected String getSortClause(final Sorting sorting) {
        String sortingRules = "";
        if (sorting != null && !sorting.isDefaultInstance()) {
            final Map<Path, Sorting.SortOrder> validSortingRules = sorting.getSortingPaths();
            if (!validSortingRules.isEmpty()) {
                final List<String> ordering = new ArrayList<>();
                // pass over the sorting rules
                validSortingRules.forEach((path, order) -> {

                    Type<?> typeClass = dictionary.lookupEntityClass(path.getPathElements().get(0).getType());
                    String prefix = getTypeAlias(typeClass);

                    for (Path.PathElement pathElement : path.getPathElements()) {
                        String fieldName = pathElement.getFieldName();

                        if (dictionary.isComplexAttribute(pathElement.getType(), fieldName)
                                || !dictionary.isRelation(pathElement.getType(), fieldName)) {
                            prefix = prefix + PERIOD + fieldName;
                        } else {
                            prefix = appendAlias(prefix, fieldName);
                        }
                    }
                    ordering.add(prefix + SPACE
                            + (order.equals(Sorting.SortOrder.desc) ? "desc" : "asc"));
                });
                sortingRules = " order by " + StringUtils.join(ordering, COMMA);
            }
        }
        return sortingRules;
    }

    /**
     * Returns whether filter expression contains toMany relationship.
     * @param filterExpression
     * @return true or false
     */
    protected boolean containsOneToMany(FilterExpression filterExpression) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor(new ArrayList<>());
        Collection<FilterPredicate> predicates = filterExpression.accept(visitor);

        return predicates.stream()
                .anyMatch(predicate -> FilterPredicate.toManyInPath(dictionary, predicate.getPath()));
    }
}
