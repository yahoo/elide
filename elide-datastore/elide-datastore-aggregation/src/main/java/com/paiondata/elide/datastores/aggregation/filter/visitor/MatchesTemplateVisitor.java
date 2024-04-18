/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.filter.visitor;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpressionVisitor;
import com.paiondata.elide.core.filter.expression.NotFilterExpression;
import com.paiondata.elide.core.filter.expression.OrFilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.visitors.FilterExpressionNormalizationVisitor;
import com.paiondata.elide.core.request.Argument;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates whether a client provided filter either:
 * 1. Directly matches a template filter.
 * 2. Contains through conjunction (logical AND) a filter expression that matches a template filter.
 * This is used to enforce table filter constraints.
 *
 * Any matching template variables are extracted from the filter expression.
 */
public class MatchesTemplateVisitor implements FilterExpressionVisitor<Boolean> {
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private FilterExpression expressionToMatch;
    private Map<String, Argument> arguments;

    public MatchesTemplateVisitor(FilterExpression expressionToMatch) {
        this.expressionToMatch = expressionToMatch;
        this.arguments = new HashMap<>();
    }

    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        return matches(expressionToMatch, filterPredicate);
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        return matches(expressionToMatch, expression)
                || expression.getLeft().accept(this)
                || expression.getRight().accept(this);
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        return matches(expressionToMatch, expression);
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        return matches(expressionToMatch, expression);
    }

    private boolean matches(FilterExpression a, FilterExpression b) {
        if (! a.getClass().equals(b.getClass())) {
            return false;
        }

        if (a instanceof AndFilterExpression) {
            AndFilterExpression andA = (AndFilterExpression) a;
            AndFilterExpression andB = (AndFilterExpression) b;

            return matches(andA.getLeft(), andB.getLeft()) && matches(andA.getRight(), andB.getRight());
        }
        if (a instanceof OrFilterExpression) {
            OrFilterExpression orA = (OrFilterExpression) a;
            OrFilterExpression orB = (OrFilterExpression) b;

            return matches(orA.getLeft(), orB.getLeft()) && matches(orA.getRight(), orB.getRight());
        }
        if (a instanceof NotFilterExpression) {
            NotFilterExpression notA = (NotFilterExpression) a;
            NotFilterExpression notB = (NotFilterExpression) b;

            return matches(notA.getNegated(), notB.getNegated());
        }
        FilterPredicate predicateA = (FilterPredicate) a;
        FilterPredicate predicateB = (FilterPredicate) b;

        boolean valueMatches = predicateA.getValues().equals(predicateB.getValues());
        boolean operatorMatches = predicateA.getOperator().equals(predicateB.getOperator());
        boolean pathMatches = pathMatches(predicateA.getPath(), predicateB.getPath());

        boolean usingTemplate = false;

        if (predicateA.getValues().size() == 1) {
            String value = predicateA.getValues().get(0).toString();
            Matcher matcher = TEMPLATE_PATTERN.matcher(value);

            usingTemplate = matcher.matches();

            if (usingTemplate && pathMatches & operatorMatches) {
                String argumentName = matcher.group(1);

                arguments.put(argumentName, Argument.builder()
                        .name(argumentName)
                        .value(predicateB.getValues().size() == 1
                                ? predicateB.getValues().get(0)
                                : predicateB.getValues())
                        .build());
            }
        }

        return (operatorMatches && pathMatches && (valueMatches || usingTemplate));
    }

    private boolean pathMatches(Path a, Path b) {
        if (a.getPathElements().size() != b.getPathElements().size()) {
            return false;
        }

        for (int idx = 0; idx < a.getPathElements().size(); idx++) {
            Path.PathElement aElement = a.getPathElements().get(idx);
            Path.PathElement bElement = b.getPathElements().get(idx);

            if (! aElement.getType().equals(bElement.getType())) {
                return false;
            }

            if (! aElement.getFieldName().equals(bElement.getFieldName())) {
                return false;
            }

            //We only compare path arguments if Path a (the template) has them.
            if (aElement.getArguments().size() > 0 && ! aElement.getArguments().equals(bElement.getArguments())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines if a client filter matches or contains a subexpression that matches a template filter.
     * @param templateFilter A templated filter expression
     * @param clientFilter The client provided filter expression.
     * @param arguments If the client filter matches, extract any table arguments.
     * @return True if the client filter matches.  False otherwise.
     */
    public static boolean isValid(
            FilterExpression templateFilter,
            FilterExpression clientFilter,
            Map<String, Argument> arguments
    ) {
        Preconditions.checkNotNull(templateFilter);

        if (clientFilter == null) {
            return false;
        }

        //First we normalize the filters so any NOT clauses are pushed down immediately in front of a predicate.
        //This lets us treat logical AND and OR without regard for any preceding NOT clauses.
        FilterExpression normalizedTemplateFilter = templateFilter.accept(new FilterExpressionNormalizationVisitor());
        FilterExpression normalizedClientFilter = clientFilter.accept(new FilterExpressionNormalizationVisitor());

        MatchesTemplateVisitor templateVisitor = new MatchesTemplateVisitor(normalizedTemplateFilter);
        boolean matches = normalizedClientFilter.accept(templateVisitor);

        if (matches) {
            arguments.putAll(templateVisitor.arguments);
        }

        return matches;
    }
}
