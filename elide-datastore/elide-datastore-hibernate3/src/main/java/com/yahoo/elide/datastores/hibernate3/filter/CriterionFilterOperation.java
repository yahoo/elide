/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterOperation;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FilterOperation that creates Hibernate Criteria from Predicates.
 */
@Slf4j
public class CriterionFilterOperation implements FilterOperation<Criterion> {

    private final Criteria criteria;

    private static String ALIAS_DELIM = "__";

    public CriterionFilterOperation(Criteria criteria) {
        this.criteria = criteria;
    }

    /**
     * For 'author.books.chapters.title', should return
     * 'author__books__chapters'.
     * @param path
     * @return
     */
    private static String getAlias(List<FilterPredicate.PathElement> path) {
        StringBuilder sb = new StringBuilder();

        FilterPredicate.PathElement first = path.get(0);
        sb.append(first.getTypeName());
        sb.append(ALIAS_DELIM);
        sb.append(first.getFieldName());

        for (int i = 1; i < path.size() - 1; i++) {
            FilterPredicate.PathElement element = path.get(i);
            sb.append(ALIAS_DELIM);
            sb.append(element.getFieldName());
        }
        return sb.toString().replace('-', '_');
    }

    /**
     * For 'author.books.chapters.title', should return
     * 'books.chapters'.
     * @param path
     * @return
     */
    private static String getAssociationPath(List<FilterPredicate.PathElement> path) {
        StringBuilder sb = new StringBuilder();

        FilterPredicate.PathElement first = path.get(0);
        sb.append(first.getFieldName());

        for (int i = 1; i < path.size() - 1; i++) {
            FilterPredicate.PathElement element = path.get(i);
            sb.append(".");
            sb.append(element.getFieldName());
        }
        return sb.toString();
    }

    @Override
    public Criterion apply(FilterPredicate filterPredicate) {
        List<FilterPredicate.PathElement> path = filterPredicate.getPath();

        /* If the predicate refers to a nested association, the restriction should be 'alias.fieldName' */
        String alias;
        if (path.size() > 1) {
            alias = getAlias(path);
            alias = alias + "." + path.get(path.size() - 1).getFieldName();
        /* If the predicate refers to the root entity, the restriction should be 'fieldName' */
        } else {
            alias = path.get(0).getFieldName();
        }

        switch (filterPredicate.getOperator()) {
            case IN:
                if (filterPredicate.getValues().isEmpty()) {
                    return Restrictions.sqlRestriction("(false)");
                }
                return Restrictions.in(alias, filterPredicate.getValues());
            case NOT:
                if (filterPredicate.getValues().isEmpty()) {
                    return Restrictions.sqlRestriction("(true)");
                }
                return Restrictions.not(Restrictions.in(alias, filterPredicate.getValues()));
            case PREFIX:
                return Restrictions.like(alias, filterPredicate.getValues().get(0) + "%");
            case PREFIX_CASE_INSENSITIVE:
                return Restrictions.ilike(alias, filterPredicate.getValues().get(0) + "%");
            case POSTFIX:
                return Restrictions.like(alias, "%" + filterPredicate.getValues().get(0));
            case POSTFIX_CASE_INSENSITIVE:
                return Restrictions.ilike(alias, "%" + filterPredicate.getValues().get(0));
            case INFIX:
                return Restrictions.like(alias, "%" + filterPredicate.getValues().get(0) + "%");
            case INFIX_CASE_INSENSITIVE:
                return Restrictions.ilike(alias, "%" + filterPredicate.getValues().get(0) + "%");
            case ISNULL:
                return Restrictions.isNull(alias);
            case NOTNULL:
                return Restrictions.isNotNull(alias);
            case LT:
                return Restrictions.lt(alias, filterPredicate.getValues().get(0));
            case LE:
                return Restrictions.le(alias, filterPredicate.getValues().get(0));
            case GT:
                return Restrictions.gt(alias, filterPredicate.getValues().get(0));
            case GE:
                return Restrictions.ge(alias, filterPredicate.getValues().get(0));
            case TRUE:
                return Restrictions.sqlRestriction("(true)");
            case FALSE:
                return Restrictions.sqlRestriction("(false)");
            default:
                throw new InvalidPredicateException("Operator not implemented: " + filterPredicate.getOperator());
        }
    }

    @Override
    public Criterion applyAll(Set<FilterPredicate> filterPredicates) {
        Criterion result = null;

        for (FilterPredicate filterPredicate : filterPredicates) {
            if (result == null) {
                result = apply(filterPredicate);
            }
            result = Restrictions.and(result, apply(filterPredicate));
        }

        return result;
    }

    public Criteria apply(FilterExpression filterExpression) {
        CriteriaVisitor visitor = new CriteriaVisitor();
        Criterion restrictions = filterExpression.accept(visitor);

        Set<String> createdAliases = new HashSet<>();

        for (FilterPredicate filterPredicate : visitor.getFilterPredicates()) {
            List<FilterPredicate.PathElement> path = filterPredicate.getPath();

            /* We only create aliases (joins) for associations outside the root entity */
            if (path.size() >= 2) {
                List<FilterPredicate.PathElement> copy = new ArrayList<>(path);
                while (copy.size() > 1) {
                    String alias = getAlias(copy);
                    String associationPath = getAssociationPath(copy);
                    if (!createdAliases.contains(alias)) {
                        criteria.createAlias(associationPath, alias);
                        createdAliases.add(alias);
                    }
                    copy = copy.subList(0, copy.size() - 1);
                }
            }
        }

        criteria.add(restrictions);
        return criteria;
    }

    /**
     * Builds a criteria from a filter expression.
     */
    public class CriteriaVisitor implements Visitor<Criterion> {
        private Criterion criterion;

        @Getter
        private final Set<FilterPredicate> filterPredicates;

        public CriteriaVisitor() {
            filterPredicates = new HashSet<>();
        }

        @Override
        public Criterion visitPredicate(FilterPredicate filterPredicate) {
            filterPredicates.add(filterPredicate);
            criterion = apply(filterPredicate);
            return criterion;
        }

        @Override
        public Criterion visitAndExpression(AndFilterExpression expression) {
            Criterion left = expression.getLeft().accept(this);
            Criterion right = expression.getRight().accept(this);
            criterion = Restrictions.and(left, right);
            return criterion;
        }

        @Override
        public Criterion visitOrExpression(OrFilterExpression expression) {
            Criterion left = expression.getLeft().accept(this);
            Criterion right = expression.getRight().accept(this);
            criterion = Restrictions.or(left, right);
            return criterion;
        }

        @Override
        public Criterion visitNotExpression(NotFilterExpression expression) {
            Criterion negated = expression.getNegated().accept(this);
            criterion =  Restrictions.not(negated);
            return criterion;
        }
    }
}
