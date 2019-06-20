/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.exceptions.InvalidValueException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
    public String generate(String columnAlias, List<FilterPredicate.FilterParameter> params) {

        if (Strings.isNullOrEmpty(columnAlias)) {
            log.error("columnAlias cannot be NULL or empty");
            throw new InvalidValueException(FILTER_PATH_NOT_NULL);
        }

        if (argumentCount == ArgumentCount.MANY) {
            Preconditions.checkState(!params.isEmpty());
        } else if (argumentCount == ArgumentCount.ONE) {
            Preconditions.checkArgument(params.size() == 1);

            if (Strings.isNullOrEmpty(params.get(0).getPlaceholder())) {
                log.error("One non-null, non-empty argument was expected.");
                throw new IllegalStateException(FILTER_ALIAS_NOT_NULL);
            }
        }

        return String.format(jpqlTemplate, upperOrLower.wrap(columnAlias), params.stream()
                .map(upperOrLower::wrap)
                .collect(Collectors.joining(COMMA)));
    }
}
