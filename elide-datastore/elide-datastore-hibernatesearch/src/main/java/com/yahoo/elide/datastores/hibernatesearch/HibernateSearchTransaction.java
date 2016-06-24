/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernatesearch;

import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.datastores.hibernate5.HibernateTransaction;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;

import java.util.Set;

/**
 * Hibernate Transaction implementation.
 */
public class HibernateSearchTransaction extends HibernateTransaction {

    private final Session session;

    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param scrollMode Scroll mode to use if scrolling enabled
     */
    protected HibernateSearchTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
        super(session, isScrollEnabled, scrollMode);
        this.session = session;
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope filterScope) {
        if (isSearch(filterScope)) {
            return runSearch(loadClass, filterScope, session);
        }
        return super.loadObjects(loadClass, filterScope);
    }

    @Override
    public <T> Iterable<T> loadObjectsWithSortingAndPagination(Class<T> entityClass, FilterScope filterScope) {
        if (isSearch(filterScope)) {
            return runSearch(entityClass, filterScope, session);
        }
        return super.loadObjectsWithSortingAndPagination(entityClass, filterScope);
    }

    public <T> Iterable<T> runSearch(Class<T> loadClass,
                                            FilterScope filterScope,
                                            Session session) {

        if (!isSearch(filterScope)) {
            throw new InvalidPredicateException("Filter is not a proper search.");
        }

        FullTextSession fullTextSession = Search.getFullTextSession(session);

        Predicate predicate =
                filterScope.getRequestScope().getPredicates().values().iterator().next().iterator().next();

        String[] fields = predicate.getField().split("\\s*,\\s*");
        String queryString = StringUtils.join(predicate.getValues(), ",");

        final MultiFieldQueryParser parser = new MultiFieldQueryParser(
                fields,
                fullTextSession.getSearchFactory().getAnalyzer(loadClass));

        try {
            Query query = fullTextSession.createFullTextQuery(
                    parser.parse(queryString));

            return query.list();
        } catch (ParseException e) {
            throw new InvalidPredicateException("Search cannot be parsed.", e);
        }
    }

    public boolean isSearch(FilterScope filterScope) {
        if (filterScope != null
                && filterScope.getRequestScope() != null
                && filterScope.getRequestScope().getPredicates() != null
                && filterScope.getRequestScope().getPredicates().size() == 1) {
            return isSearch(filterScope.getRequestScope().getPredicates().values().iterator().next());
        }
        return false;
    }

    public boolean isSearch(Set<Predicate> predicates) {
        if (predicates != null && predicates.size() == 1) {
            return isSearch(predicates.iterator().next());
        }
        return false;
    }

    public boolean isSearch(Predicate predicate) {
        return (predicate != null && predicate.getOperator().getString().equalsIgnoreCase(Operator.SEARCH.getString()));
    }
}
