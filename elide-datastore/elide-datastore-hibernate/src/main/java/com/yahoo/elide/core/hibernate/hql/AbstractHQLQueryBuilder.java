/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.Session;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

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
    protected static final String UNDERSCORE = "_";
    protected static final String PERIOD = ".";
    protected static final String COMMA = ",";
    protected static final String FROM = " FROM ";
    protected static final String JOIN = " JOIN ";
    protected static final String LEFT = " LEFT";
    protected static final String FETCH = "FETCH ";
    protected static final String SELECT = "SELECT ";
    protected static final String AS = " AS ";
    protected static final String DISTINCT = "DISTINCT ";

    protected static final boolean USE_ALIAS = true;
    protected static final boolean NO_ALIAS = false;
    protected Set<String> alreadyJoined = new HashSet<>();
    protected Set<String> alreadyJoinedAliases = new HashSet<>();

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
                .map(predicate -> extractJoinClause(predicate, skipFetches))
                .collect(Collectors.joining(SPACE));
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
     * Extracts a join clause from a filter predicate (if it exists).
     * @param predicate The predicate to examine
     * @param skipFetches Don't fetch join
     * @return A HQL string representing the join
     */
    private String extractJoinClause(FilterPredicate predicate, boolean skipFetches) {
        final Path path = predicate.getPath();
        return joinClauseFromPath(path, skipFetches);
    }

    private String joinClauseFromPath(Path path, boolean skipFetches) {
        StringBuilder joinClause = new StringBuilder();
        String previousAlias = null;
        for (Path.PathElement pathElement : path.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Class<?> typeClass = dictionary.lookupEntityClass(pathElement.getType());
            String typeAlias = FilterPredicate.getTypeAlias(typeClass);

            //Nothing left to join.
            if (! dictionary.isRelation(pathElement.getType(), fieldName)) {
                return joinClause.toString();
            }

            String alias = typeAlias + UNDERSCORE + fieldName;

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
                fetch = FETCH;

            }
            String joinFragment = LEFT + JOIN + fetch + joinKey + SPACE + alias + SPACE;

            if (!alreadyJoined.contains(joinKey)) {
                joinClause.append(joinFragment);
                alreadyJoined.add(joinKey);
                alreadyJoinedAliases.add(alias);
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
                String relationshipAlias = alias + UNDERSCORE + relationshipName;

                if (alreadyJoined.contains(joinKey)) {
                    continue;
                }

                alreadyJoined.add(joinKey);
                alreadyJoinedAliases.add(relationshipAlias);
                joinString.append(LEFT);
                joinString.append(JOIN);
                joinString.append(FETCH);
                joinString.append(joinKey);
                joinString.append(SPACE);
                joinString.append(relationshipAlias);
                joinString.append(SPACE);
            }
        }
        return joinString.toString();
    }

    /**
     * Builds a explicit LEFT JOIN clauses instead of implicit sorting joins.
     * @param sorting The sorting object passed from the client
     * @param sortClass The class to sort.
     * @return The JOIN clause that can be added to the FROM clause.
     */
    protected String explicitSortJoins(final Optional<Sorting> sorting, Class<?> sortClass) {
        StringBuilder joinClause = new StringBuilder();
        if (sorting.isPresent() && !sorting.get().isDefaultInstance()) {
            final Map<Path, Sorting.SortOrder> validSortingRules = sorting.get().getValidSortingRules(
                sortClass, dictionary
            );
            for (Map.Entry<Path, Sorting.SortOrder> entry : validSortingRules.entrySet()) {
                Path path = entry.getKey();
                joinClause.append(joinClauseFromPath(path, true));
            }
        }
        return joinClause.toString();
    }

    /**
     * Returns a sorting object into a HQL ORDER BY string.
     * @param sorting The sorting object passed from the client
     * @param sortClass The class to sort.
     * @param prefixWithAlias Whether the sorting fields should be prefixed by an alias.
     * @return The sorting clause
     */
    protected String getSortClause(final Optional<Sorting> sorting, Class<?> sortClass, boolean prefixWithAlias) {
        String sortingRules = "";
        if (sorting.isPresent() && !sorting.get().isDefaultInstance()) {
            final Map<Path, Sorting.SortOrder> validSortingRules = sorting.get().getValidSortingRules(
                    sortClass, dictionary
            );
            if (!validSortingRules.isEmpty()) {
                final List<String> ordering = new ArrayList<>();
                // pass over the sorting rules
                validSortingRules.entrySet().stream().forEachOrdered(entry -> {
                        Path path = entry.getKey();

                        String orderElement;
                        if (alreadyJoinedAliases.contains(path.getAlias())) {
                            orderElement = path.lastElement()
                                .map(element -> path.getAlias() + PERIOD + element.getFieldName())
                                .orElse("");
                        } else {
                            String prefix = (prefixWithAlias) ? Path.getTypeAlias(sortClass) + PERIOD : "";
                            orderElement = prefix + path.getFieldPath();
                        }
                        orderElement += SPACE + (entry.getValue().equals(Sorting.SortOrder.desc) ? "desc" : "asc");
                        ordering.add(orderElement);
                    }
                );
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
