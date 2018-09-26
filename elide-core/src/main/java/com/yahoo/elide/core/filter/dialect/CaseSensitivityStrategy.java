/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.filter.Operator;

/**
 * Defines the behavior how string comparisons should be compared regarding case sensitivity.
 */
public interface CaseSensitivityStrategy {
    /**
     * Maps a general case operator to an operator respecting case sensitivity behavior.
     *
     * @param baseOperator the general case operator (case sensitivity not considered)
     * @return comparison operator (case sensitivity considered)
     */
    Operator mapOperator(Operator baseOperator);

    /**
     * The default Elide strategy which implements the FIQL standard.
     * Uses lowercase string comparisons.
     * (@see <a href="https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00#section-3.2.2.1">
     * FIQL specification</a>)
     */
    public class FIQLCompliant implements CaseSensitivityStrategy {
        public Operator mapOperator(Operator operator) {
            switch (operator) {
                case IN:
                    return Operator.IN_INSENSITIVE;
                case NOT:
                    return Operator.NOT_INSENSITIVE;
                case INFIX:
                    return Operator.INFIX_CASE_INSENSITIVE;
                case PREFIX:
                    return Operator.PREFIX_CASE_INSENSITIVE;
                case POSTFIX:
                    return Operator.POSTFIX_CASE_INSENSITIVE;
                default:
                    return operator;
            }
        }
    }

    /**
     * The strategy delegates the decision case sensitivity for string comparison to the
     * underlying database column collation definition.
     * <p>
     * This strategy can be used if the underlying database has performance issues due to
     * missing functional index functionality (i.e. MySQL).
     */
    public class UseColumnCollation implements CaseSensitivityStrategy {
        public Operator mapOperator(Operator operator) {
            return operator;
        }
    }
}
