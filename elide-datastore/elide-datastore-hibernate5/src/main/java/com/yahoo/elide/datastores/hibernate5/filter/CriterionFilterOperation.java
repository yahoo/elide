/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterOperation;
import com.yahoo.elide.core.filter.Predicate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Set;

/**
 * FilterOperation that creates Hibernate Criterions from Predicates.
 */
public class CriterionFilterOperation implements FilterOperation<Criterion> {
    @Override
    public Criterion apply(Predicate predicate) {
        switch (predicate.getOperator()) {
            case IN:
                return Restrictions.in(predicate.getField(), predicate.getValues());
            case NOT:
                return Restrictions.not(Restrictions.in(predicate.getField(), predicate.getValues()));
            case PREFIX:
                return Restrictions.like(predicate.getField(), predicate.getValues().get(0) + "%");
            case POSTFIX:
                return Restrictions.like(predicate.getField(), "%" + predicate.getValues().get(0));
            case INFIX:
                return Restrictions.like(predicate.getField(), "%" + predicate.getValues().get(0) + "%");
            case ISNULL:
                return Restrictions.isNull(predicate.getField());
            case NOTNULL:
                return Restrictions.isNotNull(predicate.getField());
            case LT:
                return Restrictions.lt(predicate.getField(), predicate.getValues().get(0));
            case LE:
                return Restrictions.le(predicate.getField(), predicate.getValues().get(0));
            case GT:
                return Restrictions.gt(predicate.getField(), predicate.getValues().get(0));
            case GE:
                return Restrictions.ge(predicate.getField(), predicate.getValues().get(0));
            default:
                throw new InvalidPredicateException("Operator not implemented: " + predicate.getOperator());
        }
    }

    @Override
    public Criterion applyAll(Set<Predicate> predicates) {
        Criterion result = null;

        for (Predicate predicate : predicates) {
            result = andWithNull(result, apply(predicate));
        }

        return result;
    }

    public static Criterion andWithNull(Criterion lhs, Criterion rhs) {
        if (lhs == null && rhs == null) {
            return null;
        } else if (lhs == null) {
            return rhs;
        } else if (rhs == null) {
            return lhs;
        } else {
            return Restrictions.and(lhs, rhs);
        }
    }
}
