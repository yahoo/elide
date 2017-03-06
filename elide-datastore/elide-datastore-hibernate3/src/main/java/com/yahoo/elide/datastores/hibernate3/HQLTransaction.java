/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generates an HQL Transaction.
 */
public class HQLTransaction {
    /**
     * HQLTransaction Builder. Can build an HQL Query taking optional filters, sorting and pagination rules.
     *
     * @param <T> The Builder Type
     */
    public static class Builder<T> {
        private final Session session;
        private final Collection collection;
        private final Class<T> entityClass;
        private final EntityDictionary dictionary;
        private Set<FilterPredicate> filters = null;
        private String sortingRules = "";
        private Pagination pagination = null;
        private FilterExpression filterExpression = null;

        public Builder(final Session session, final Collection collection, final Class<T> entityClass,
                       final EntityDictionary dictionary) {
            this.session = session;
            this.collection = collection;
            this.entityClass = entityClass;
            this.dictionary = dictionary;
        }

        public Builder withPossibleFilterExpression(Optional<FilterExpression> filterExpression) {
            if (filterExpression.isPresent()) {
                return withFilterExpression(filterExpression.get());
            }
            return this;
        }

        public Builder withFilterExpression(FilterExpression filterExpression) {
            this.filterExpression = filterExpression;
            PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
            this.filters = filterExpression.accept(visitor);
            return this;
        }

        public Builder withPossibleFilters(final Optional<Set<FilterPredicate>> possibleFilters) {
            if (possibleFilters.isPresent()) {
                return withFilters(possibleFilters.get());
            }
            return this;
        }

        public Builder withFilters(final Set<FilterPredicate> filters) {
            if (filters != null && !filters.isEmpty()) {
                this.filters = filters;
            }
            return this;
        }

        public Builder withPossibleSorting(final Optional<Sorting> possibleSorting) {
            if (possibleSorting.isPresent()) {
                return withSorting(possibleSorting.get());
            }
            return this;
        }

        public Builder withSorting(final Sorting sorting) {
            if (sorting != null && !sorting.isDefaultInstance()) {
                final Map<String, Sorting.SortOrder> validSortingRules = sorting.getValidSortingRules(
                        entityClass, dictionary
                );
                if (!validSortingRules.isEmpty()) {
                    final List<String> ordering = new ArrayList<>();
                    // pass over the sorting rules
                    validSortingRules.entrySet().stream().forEachOrdered(entry ->
                            ordering.add(entry.getKey() + " " + (entry.getValue().equals(Sorting.SortOrder.desc)
                                    ? "desc"
                                    : "asc"))
                    );
                    sortingRules += "order by " + StringUtils.join(ordering, ",");
                }
            }
            return this;
        }

        public Builder withPossiblePagination(final Optional<Pagination> possiblePagination) {
            if (possiblePagination.isPresent()) {
                return withPagination(possiblePagination.get());
            }
            return this;
        }

        public Builder withPagination(final Pagination pagination) {
            if (pagination != null) {
                this.pagination = pagination;
            }
            return this;
        }

        public Optional<Query> build() {
            String filterString = "";

            // apply filtering - eg where clause's
            if (filterExpression != null) {
                filterString += new HQLFilterOperation().apply(filterExpression);
            } else if (filters != null) {
                filterString += new HQLFilterOperation().applyAll(filters);
            }

            // add sorting into HQL string query generation
            if (sortingRules != null && !sortingRules.isEmpty()) {
                filterString += sortingRules;
            }


            Query query = null;
            if (filterString.length() != 0) {
                query = session.createFilter(collection, filterString);

                if (filters != null) {
                    for (FilterPredicate filterPredicate : filters) {
                        if (filterPredicate.getOperator().isParameterized()) {
                            String name = filterPredicate.getParameterName();
                            query = query.setParameterList(name, filterPredicate.getValues());
                        }
                    }
                }
            }
            if (pagination != null) {

                if (query == null) {
                    query = session.createFilter(collection, "");
                }
                query.setFirstResult(pagination.getOffset());
                query.setMaxResults(pagination.getLimit());
            }
            return Optional.ofNullable(query);
        }
    }
}
