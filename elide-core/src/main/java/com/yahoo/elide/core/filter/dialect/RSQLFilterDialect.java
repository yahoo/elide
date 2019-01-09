/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.IsNullPredicate;
import com.yahoo.elide.core.filter.NotNullPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.parsers.JsonApiParser;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.google.common.collect.ImmutableMap;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/**
 * FilterDialect which implements support for RSQL filter dialect.
 */
public class RSQLFilterDialect implements SubqueryFilterDialect, JoinFilterDialect {
    private static final String SINGLE_PARAMETER_ONLY = "There can only be a single filter query parameter";
    private static final String INVALID_QUERY_PARAMETER = "Invalid query parameter: ";
    private static final Pattern TYPED_FILTER_PATTERN = Pattern.compile("filter\\[([^\\]]+)\\]");
    private static final ComparisonOperator ISNULL_OP = new ComparisonOperator("=isnull=", false);

    /* Subset of operators that map directly to Elide operators */
    private static final Map<ComparisonOperator, Operator> OPERATOR_MAP =
            ImmutableMap.<ComparisonOperator, Operator>builder()
                    .put(RSQLOperators.LESS_THAN, Operator.LT)
                    .put(RSQLOperators.GREATER_THAN, Operator.GT)
                    .put(RSQLOperators.GREATER_THAN_OR_EQUAL, Operator.GE)
                    .put(RSQLOperators.LESS_THAN_OR_EQUAL, Operator.LE)
                    .build();


    private final RSQLParser parser;
    private final EntityDictionary dictionary;
    private final CaseSensitivityStrategy caseSensitivityStrategy;

    public RSQLFilterDialect(EntityDictionary dictionary) {
        this(dictionary, new CaseSensitivityStrategy.FIQLCompliant());
    }

    public RSQLFilterDialect(EntityDictionary dictionary, CaseSensitivityStrategy caseSensitivityStrategy) {
        parser = new RSQLParser(getDefaultOperatorsWithIsnull());
        this.dictionary = dictionary;
        this.caseSensitivityStrategy = caseSensitivityStrategy;
    }

    //add rsql isnull op to the default ops
    private static Set<ComparisonOperator> getDefaultOperatorsWithIsnull() {
        Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
        operators.add(ISNULL_OP);
        return operators;
    }

    @Override
    public FilterExpression parseGlobalExpression(String path, MultivaluedMap<String, String> filterParams)
            throws ParseException {
        if (filterParams.size() != 1) {
            throw new ParseException(SINGLE_PARAMETER_ONLY);
        }

        MultivaluedMap.Entry<String, List<String>> entry = filterParams.entrySet().iterator().next();
        String queryParamName = entry.getKey();

        if (!"filter".equals(queryParamName)) {
            throw new ParseException(INVALID_QUERY_PARAMETER + queryParamName);
        }
        List<String> queryParamValues = entry.getValue();

        if (queryParamValues.size() != 1) {
            throw new ParseException(SINGLE_PARAMETER_ONLY);
        }

        String queryParamValue = queryParamValues.get(0);

        /*
         * Extract the last collection in the URL.
         */
        String normalizedPath = JsonApiParser.normalizePath(path);
        String[] pathComponents = normalizedPath.split("/");
        String lastPathComponent = pathComponents.length > 0 ? pathComponents[pathComponents.length - 1] : "";

        /*
         * TODO - create a visitor which extracts the type/class of the last path component.
         * This works today by virtue that global filter expressions are only used for root collections
         * and NOT nested associations.
         */
        Class entityType = dictionary.getEntityClass(lastPathComponent);
        if (entityType == null) {
            throw new ParseException("No such collection: " + lastPathComponent);
        }

        try {
            Node ast = parser.parse(queryParamValue);
            RSQL2FilterExpressionVisitor visitor = new RSQL2FilterExpressionVisitor(true);
            return ast.accept(visitor, entityType);
        } catch (RSQLParserException e) {
            throw new ParseException(e.getMessage());
        }
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String> filterParams)
            throws ParseException {

        Map<String, FilterExpression> expressionByType = new HashMap<>();

        for (MultivaluedMap.Entry<String, List<String>> entry : filterParams.entrySet()) {

            String paramName = entry.getKey();
            List<String> paramValues = entry.getValue();

            Matcher matcher = TYPED_FILTER_PATTERN.matcher(paramName);
            if (matcher.find()) {
                String typeName = matcher.group(1);
                if (paramValues.size() != 1) {
                    throw new ParseException("Exactly one RSQL expression must be defined for type : " + typeName);
                }

                Class entityType = dictionary.getEntityClass(typeName);
                if (entityType == null) {
                    throw new ParseException(INVALID_QUERY_PARAMETER + paramName);
                }

                String expressionText = paramValues.get(0);
                try {
                    Node ast = parser.parse(expressionText);
                    RSQL2FilterExpressionVisitor visitor = new RSQL2FilterExpressionVisitor(false);
                    FilterExpression filterExpression = ast.accept(visitor, entityType);
                    expressionByType.put(typeName, filterExpression);
                } catch (RSQLParserException e) {
                    throw new ParseException(e.getMessage());
                }
            } else {
                throw new ParseException(INVALID_QUERY_PARAMETER + paramName);
            }
        }
        return expressionByType;
    }

    /**
     * Allows base RSQLParseException to carry a parametrized message.
     */
    public static class RSQLParseException extends RSQLParserException {
        private String message;

        RSQLParseException(String message) {
            super(null);
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    /**
     * Visitor which converts RSQL abstract syntax tree into an Elide filter expression.
     */
    public class RSQL2FilterExpressionVisitor implements RSQLVisitor<FilterExpression, Class> {
        private boolean allowNestedToManyAssociations = false;

        public RSQL2FilterExpressionVisitor(boolean allowNestedToManyAssociations) {
            this.allowNestedToManyAssociations = allowNestedToManyAssociations;
        }

        private Path buildPath(Class rootEntityType, String selector) {
            String[] associationNames = selector.split("\\.");

            List<Path.PathElement> path = new ArrayList<>();
            Class entityType = rootEntityType;

            for (String associationName : associationNames) {
                String typeName = dictionary.getJsonAliasFor(entityType);
                Class fieldType = dictionary.getParameterizedType(entityType, associationName);

                if (fieldType == null) {
                    throw new RSQLParseException(
                            String.format("No such association %s for type %s", associationName, typeName));
                }

                path.add(new Path.PathElement(entityType, fieldType, associationName));

                entityType = fieldType;
            }
            return new Path(path);
        }

        @Override
        public FilterExpression visit(AndNode node, Class entityType) {

            List<Node> children = node.getChildren();
            if (children.size() < 2) {
                throw new RSQLParseException("Logical AND requires two arguments");
            }
            FilterExpression left = children.get(0).accept(this, entityType);
            FilterExpression right = children.get(1).accept(this, entityType);

            AndFilterExpression andFilterExpression = new AndFilterExpression(left, right);

            for (int idx = 2; idx < children.size(); idx++) {
                right = children.get(idx).accept(this, entityType);
                andFilterExpression = new AndFilterExpression(andFilterExpression, right);
            }

            return andFilterExpression;
        }

        @Override
        public FilterExpression visit(OrNode node, Class entityType) {

            List<Node> children = node.getChildren();
            if (children.size() < 2) {
                throw new RSQLParseException("Logical OR requires two arguments");
            }
            FilterExpression left = children.get(0).accept(this, entityType);
            FilterExpression right = children.get(1).accept(this, entityType);

            OrFilterExpression orFilterExpression = new OrFilterExpression(left, right);

            for (int idx = 2; idx < children.size(); idx++) {
                right = children.get(idx).accept(this, entityType);
                orFilterExpression = new OrFilterExpression(orFilterExpression, right);
            }

            return orFilterExpression;
        }

        @Override
        public FilterExpression visit(ComparisonNode node, Class entityType) {
            ComparisonOperator op = node.getOperator();
            String relationship = node.getSelector();
            List<String> arguments = node.getArguments();
            Path path = buildPath(entityType, relationship);

            if (FilterPredicate.toManyInPath(dictionary, path) && !allowNestedToManyAssociations) {
                throw new RSQLParseException(String.format("Invalid association %s", relationship));
            }

            //handles '=isnull=' op before coerce arguments
            if (op.equals(ISNULL_OP)) {
                return buildIsNullOperator(path, arguments);
            }

            Class<?> relationshipType = path.lastElement()
                    .map(Path.PathElement::getFieldType)
                    .orElseThrow(() -> new IllegalStateException("Path must not be empty"));

            //Coerce arguments to their correct types
            List<Object> values = arguments.stream()
                    .map((argument) -> (Object) CoerceUtil.coerce(argument, relationshipType))
                    .collect(Collectors.toList());

            if (op.equals(RSQLOperators.EQUAL) || op.equals(RSQLOperators.IN)) {
                return equalityExpression(arguments.get(0), path, values);
            }
            if (op.equals(RSQLOperators.NOT_EQUAL) || op.equals(RSQLOperators.NOT_IN)) {
                return new NotFilterExpression(equalityExpression(arguments.get(0), path, values));
            }
            if (OPERATOR_MAP.containsKey(op)) {
                return new FilterPredicate(path, OPERATOR_MAP.get(op), values);
            }

            throw new RSQLParseException(String.format("Invalid Operator %s", op.getSymbol()));
        }

        private FilterExpression equalityExpression(String argument, Path path, List<Object> values) {
            boolean startsWith = argument.startsWith("*");
            boolean endsWith = argument.endsWith("*");
            if (startsWith && endsWith && argument.length() > 2) {
                String value = argument.substring(1, argument.length() - 1);
                return new FilterPredicate(path, caseSensitivityStrategy.mapOperator(Operator.INFIX),
                        Collections.singletonList(value));
            }
            if (startsWith && argument.length() > 1) {
                String value = argument.substring(1, argument.length());
                return new FilterPredicate(path, caseSensitivityStrategy.mapOperator(Operator.POSTFIX),
                        Collections.singletonList(value));
            }
            if (endsWith && argument.length() > 1) {
                String value = argument.substring(0, argument.length() - 1);
                return new FilterPredicate(path, caseSensitivityStrategy.mapOperator(Operator.PREFIX),
                        Collections.singletonList(value));
            }

            Boolean isStringLike = path.lastElement()
                    .map(e -> e.getFieldType().isAssignableFrom(String.class))
                    .orElse(false);
            if (isStringLike) {
                return new FilterPredicate(path, caseSensitivityStrategy.mapOperator(Operator.IN), values);
            }

            return new InPredicate(path, values);
        }

        /**
         * Returns Predicate for '=isnull=' case depending on its arguments.
         * <p>
         * NOTE: Filter Expression builder specially for '=isnull=' case.
         *
         * @return Returns Predicate for '=isnull=' case depending on its arguments.
         */
        private FilterExpression buildIsNullOperator(Path path, List<String> arguments) {
            String arg = arguments.get(0);
            try {
                boolean wantsNull = CoerceUtil.coerce(arg, boolean.class);
                if (wantsNull) {
                    return new IsNullPredicate(path);
                }
                return new NotNullPredicate(path);
            } catch (InvalidValueException ignored) {
                throw new RSQLParseException(String.format("Invalid value for operator =isnull= '%s'", arg));
            }
        }
    }
}
