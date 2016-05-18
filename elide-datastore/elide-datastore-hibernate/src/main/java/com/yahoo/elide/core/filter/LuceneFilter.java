/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;

import java.util.Set;

/**
 * Created by cwilliamson on 5/10/16.
 */
public class LuceneFilter {

    public static <T> Iterable<T> runSearch(Class<T> loadClass,
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

    public static boolean isSearch(FilterScope filterScope) {
        if (filterScope != null
            && filterScope.getRequestScope() != null
            && filterScope.getRequestScope().getPredicates() != null
            && filterScope.getRequestScope().getPredicates().size() == 1) {
            return isSearch(filterScope.getRequestScope().getPredicates().values().iterator().next());
        }
        return false;
    }

    public static boolean isSearch(Set<Predicate> predicates) {
        if (predicates != null && predicates.size() == 1) {
            return isSearch(predicates.iterator().next());
        }
        return false;
    }

    public static boolean isSearch(Predicate predicate) {
        return (predicate != null && predicate.getOperator().getString().equalsIgnoreCase(Operator.SEARCH.getString()));
    }
}
