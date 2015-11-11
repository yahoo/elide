/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Operator enum for predicates.
 */
@RequiredArgsConstructor
public enum Operator {
    IN("in"),
    NOT("not"),
    PREFIX("prefix"),
    POSTFIX("postfix"),
    INFIX("infix");

    @Getter private final String string;

    /**
     * Returns Operator from query parameter operator string.
     *
     * @param string operator string from query parameter
     * @return Operator
     */
    public static Operator fromString(final String string) {
        for (final Operator operator : values()) {
            if (operator.getString().equals(string)) {
                return operator;
            }
        }

        throw new InvalidPredicateException("Unknown operator in filter: " + string);
    }
}
