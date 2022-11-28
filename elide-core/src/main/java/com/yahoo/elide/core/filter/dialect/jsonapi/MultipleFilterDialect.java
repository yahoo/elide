/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect.jsonapi;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

/**
 * A filter dialect that supports an ordered list of different dialects.  Dialects
 * are attempted in sequence.  The first dialect that successfully parses a filter expression
 * is used.  If no dialect succeeds, the error from the last dialect is returned.
 */
@AllArgsConstructor
@Slf4j
public class MultipleFilterDialect implements JoinFilterDialect, SubqueryFilterDialect {
    private List<JoinFilterDialect> joinDialects;
    private List<SubqueryFilterDialect> subqueryDialects;

    public MultipleFilterDialect(EntityDictionary dictionary) {
        DefaultFilterDialect defaultDialect = new DefaultFilterDialect(dictionary);
        joinDialects = new ArrayList<>();
        joinDialects.add(defaultDialect);
        subqueryDialects = new ArrayList<>();
        subqueryDialects.add(defaultDialect);
    }

    @Override
    public FilterExpression parseGlobalExpression(String path,
                                                  MultivaluedMap<String, String> queryParams,
                                                  String apiVersion) throws ParseException {
        if (joinDialects.isEmpty()) {
            throw new ParseException("Heterogeneous type filtering not supported");
        }

        return parseExpression(joinDialects, (dialect) -> dialect.parseGlobalExpression(path, queryParams, apiVersion));
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path,
                                                              MultivaluedMap<String, String> queryParams,
                                                              String apiVersion)
            throws ParseException {

        if (subqueryDialects.isEmpty()) {
            throw new ParseException("Type filtering not supported");
        }

        return parseExpression(subqueryDialects, (dialect) -> dialect.parseTypedExpression(path,
                queryParams, apiVersion));
    }

    private static <T, R> R parseExpression(List<T> dialects, ParseFunction<T, R> parseFunction) throws ParseException {
        ParseException lastFailure = null;
        for (T dialect : dialects) {
            try {
                return parseFunction.apply(dialect);
            } catch (ParseException e) {
                if (log.isTraceEnabled()) {
                    log.trace("Parse Failure: {}", e.getMessage());
                }
                if (lastFailure != null) {
                    ParseException prev = lastFailure;
                    lastFailure = new ParseException(e.getMessage() + "\n" + lastFailure.getMessage());
                    lastFailure.addSuppressed(prev);
                    lastFailure.addSuppressed(e);
                } else {
                    lastFailure = e;
                }
            }
        }
        if (lastFailure == null) {
            lastFailure = new ParseException("No dialects");
        }
        throw lastFailure;
    }

    /**
     * A dialect parse function.
     *
     * @param <T> The parser dialect
     * @param <R> the return type of the parser
     */
    @FunctionalInterface
    public interface ParseFunction<T, R> {
        R apply(T dialect) throws ParseException;
    }
}
