/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import static com.yahoo.elide.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Sorting;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract class used to construct HQL queries.
 */
public abstract class AbstractHQLQueryBuilder {
    protected final Session session;
    protected final EntityDictionary dictionary;

    protected Optional<Sorting> sorting;
    protected Optional<Pagination> pagination;
    protected Optional<FilterExpression> filterExpression;
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
        public Class<?> getParentType();

        public Class<?> getChildType();

        public String getRelationshipName();

        public Object getParent();

        public Collection<?> getChildren();
    }

    public AbstractHQLQueryBuilder(EntityDictionary dictionary, Session session) {
        this.session = session;
        this.dictionary = dictionary;

        sorting = Optional.empty();
        pagination = Optional.empty();
        filterExpression = Optional.empty();
    }

    public abstract Query build();

    public AbstractHQLQueryBuilder withPossibleFilterExpression(Optional<FilterExpression> filterExpression) {
        this.filterExpression = filterExpression;
        return this;
    }

    public AbstractHQLQueryBuilder withPossibleSorting(final Optional<Sorting> possibleSorting) {
        this.sorting = possibleSorting;
        return this;
    }

    public AbstractHQLQueryBuilder withPossiblePagination(final Optional<Pagination> possiblePagination) {
        this.pagination = possiblePagination;
        return this;
    }

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
                filterPredicate.getParameters().forEach(param -> {
                    query.setParameter(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue());
                });
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
    protected String getJoinClauseFromSort(Optional<Sorting> sorting) {
        return getJoinClauseFromSort(sorting, false);
    }

    /**
     * Extracts all the HQL JOIN clauses from given filter expression.
     * @param sorting the sort expression to extract a join clause from
     * @param skipFetches JOIN but don't FETCH JOIN a relationship.
     * @return an HQL join clause
     */
    protected String getJoinClauseFromSort(Optional<Sorting> sorting, boolean skipFetches) {
        if (sorting.isPresent() && !sorting.get().isDefaultInstance()) {
            Map<Path, Sorting.SortOrder> validSortingRules = sorting.get().getSortingPaths();
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
        if (pagination.isPresent()) {
            Pagination pagination = this.pagination.get();
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
            Class<?> typeClass = dictionary.lookupEntityClass(pathElement.getType());
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
            if (!skipFetches && type.isToOne() && !type.isComputed() && previousAlias == null) {
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
    protected String extractToOneMergeJoins(Class<?> entityClass, String alias) {
        return extractToOneMergeJoins(entityClass, alias, (unused) -> false);
    }

    protected String extractToOneMergeJoins(Class<?> entityClass, String alias,
                                            Function<String, Boolean> skipRelation) {
        List<String> relationshipNames = dictionary.getRelationships(entityClass);
        StringBuilder joinString = new StringBuilder("");
        for (String relationshipName : relationshipNames) {
            RelationshipType type = dictionary.getRelationshipType(entityClass, relationshipName);
            if (type.isToOne() && !type.isComputed()) {
                if (skipRelation.apply(relationshipName)) {
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
    protected String getSortClause(final Optional<Sorting> sorting) {
        String sortingRules = "";
        if (sorting.isPresent() && !sorting.get().isDefaultInstance()) {
            final Map<Path, Sorting.SortOrder> validSortingRules = sorting.get().getSortingPaths();
            if (!validSortingRules.isEmpty()) {
                final List<String> ordering = new ArrayList<>();
                // pass over the sorting rules
                validSortingRules.forEach((path, order) -> {
                    String previousAlias = null;
                    String aliasedFieldName = null;

                    for (Path.PathElement pathElement : path.getPathElements()) {
                        String fieldName = pathElement.getFieldName();
                        Class<?> typeClass = dictionary.lookupEntityClass(pathElement.getType());
                        previousAlias = previousAlias == null
                                ? getTypeAlias(typeClass)
                                : previousAlias;
                        aliasedFieldName = previousAlias + PERIOD + fieldName;

                        if (!dictionary.isRelation(pathElement.getType(), fieldName)) {
                            break;
                        }

                        previousAlias = appendAlias(previousAlias, fieldName);
                    }
                    ordering.add(aliasedFieldName + SPACE
                            + (order.equals(Sorting.SortOrder.desc) ? "desc" : "asc"));
                });
                sortingRules = " order by " + StringUtils.join(ordering, COMMA);
            }
        }
        return sortingRules;
    }

    /**
     * Returns whether filter expression contains toMany relationship
     * @param filterExpression
     * @return
     */
    protected boolean containsOneToMany(FilterExpression filterExpression) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor(new ArrayList<>());
        Collection<FilterPredicate> predicates = filterExpression.accept(visitor);

        return predicates.stream()
                .anyMatch(predicate -> FilterPredicate.toManyInPath(dictionary, predicate.getPath()));
    }
}
