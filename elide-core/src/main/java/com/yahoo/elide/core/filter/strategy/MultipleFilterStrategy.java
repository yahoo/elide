/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.strategy;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A filter strategy that supports an ordered list of different strategies.  Strategies
 * are attempted in sequence.  The first strategy that successfully parses a filter expression
 * is used.  If no strategy succeeds, the error from the last strategy is returned.
 */
@AllArgsConstructor
@Slf4j
public class MultipleFilterStrategy implements JoinFilterStrategy, SubqueryFilterStrategy {
    private List<JoinFilterStrategy> joinStrategies;
    private List<SubqueryFilterStrategy> subqueryStrategies;

    public MultipleFilterStrategy(EntityDictionary dictionary) {
        DefaultFilterStrategy defaultStrategy = new DefaultFilterStrategy(dictionary);
        joinStrategies = new ArrayList<>();
        joinStrategies.add(defaultStrategy);
        subqueryStrategies = new ArrayList<>();
        subqueryStrategies.add(defaultStrategy);
    }

    @Override
    public FilterExpression parseGlobalExpression(String path,
                                            MultivaluedMap<String, String> queryParams) throws ParseException {
        if (joinStrategies.isEmpty()) {
            throw new ParseException("Heterogeneous type filtering not supported");
        }

        ParseException lastFailure = null;
        for (JoinFilterStrategy strategy : joinStrategies) {
            try {
                return strategy.parseGlobalExpression(path, queryParams);
            } catch (ParseException e) {
                log.trace("Parse Failure: {}", e.getMessage());
                if (lastFailure != null) {
                    lastFailure = new ParseException(e.getMessage() + "\n" + lastFailure.getMessage());
                } else {
                    lastFailure = e;
                }
            }
        }
        throw lastFailure;
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path,
                                                        MultivaluedMap<String, String> queryParams)
            throws ParseException {

        if (subqueryStrategies.isEmpty()) {
            throw new ParseException("Type filtering not supported");
        }

        ParseException lastFailure = null;
        for (SubqueryFilterStrategy strategy : subqueryStrategies) {
            try {
                return strategy.parseTypedExpression(path, queryParams);
            } catch (ParseException e) {
                log.trace("Parse Failure: {}", e.getMessage());
                if (lastFailure != null) {
                    lastFailure = new ParseException(e.getMessage() + "\n" + lastFailure.getMessage());
                } else {
                    lastFailure = e;
                }
            }
        }
        throw lastFailure;
    }
}
