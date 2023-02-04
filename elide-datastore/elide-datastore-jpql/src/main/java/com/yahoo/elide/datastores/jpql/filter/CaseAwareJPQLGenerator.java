/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPQLPredicateGenerator that can generate both case sensitive/insensitive predicate fragments.
 */
@Slf4j
public class CaseAwareJPQLGenerator implements JPQLPredicateGenerator {

    private static final String COMMA = ", ";
    private static final String FILTER_PATH_NOT_NULL = "Filtering field path cannot be empty.";
    private static final String FILTER_ALIAS_NOT_NULL = "Filtering alias cannot be empty.";

    private Case upperOrLower;
    private String jpqlTemplate;
    private ArgumentCount argumentCount;

    /**
     * Whether to use uppercase, lowercase, or not transformation for case insensitive queries.
     */
    public enum Case {
        LOWER,
        UPPER,
        NONE;

        public String wrap(String toWrap) {
            if (this.equals(NONE)) {
                return toWrap;
            }

            return String.format("%s(%s)", this.name(), toWrap);
        }

        public String wrap(FilterPredicate.FilterParameter toWrap) {
            return wrap(toWrap.getPlaceholder());
        }
    }

    /**
     * The number of values that are passed to a filter predicate.
     */
    public enum ArgumentCount {
        ZERO,
        ONE,
        MANY
    }

    /**
     * Constructor.
     * @param jpqlTemplate A JPQL Query Fragment Template
     * @param upperOrLower UPPER, LOWER, or NONE
     * @param argumentCount ZERO, ONE, or MANY
     */
    public CaseAwareJPQLGenerator(String jpqlTemplate, Case upperOrLower, ArgumentCount argumentCount) {
        this.upperOrLower = upperOrLower;
        this.jpqlTemplate = jpqlTemplate;
        this.argumentCount = argumentCount;
    }

    @Override
    public String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator) {
       String columnAlias = aliasGenerator.apply(predicate.getPath());
       List<FilterPredicate.FilterParameter> parameters = predicate.getParameters();

        if (StringUtils.isEmpty(columnAlias)) {
            log.error("columnAlias cannot be NULL or empty");
            throw new InvalidValueException(FILTER_PATH_NOT_NULL);
        }

        if (argumentCount == ArgumentCount.MANY) {
            Preconditions.checkState(!parameters.isEmpty());
        } else if (argumentCount == ArgumentCount.ONE) {
            Preconditions.checkArgument(parameters.size() == 1);

            if (StringUtils.isEmpty(parameters.get(0).getPlaceholder())) {
                log.error("One non-null, non-empty argument was expected.");
                throw new IllegalStateException(FILTER_ALIAS_NOT_NULL);
            }
        }

        return String.format(jpqlTemplate, upperOrLower.wrap(columnAlias), parameters.stream()
                .map(upperOrLower::wrap)
                .collect(Collectors.joining(COMMA)));
    }
}
