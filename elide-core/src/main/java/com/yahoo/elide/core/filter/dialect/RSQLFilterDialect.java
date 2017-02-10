/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.google.common.collect.ImmutableMap;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Getter;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FilterDialect which implements support for RSQL filter dialect.
 */
public class RSQLFilterDialect implements SubqueryFilterDialect, JoinFilterDialect {

    private static final Pattern TYPED_FILTER_PATTERN = Pattern.compile("filter\\[([^\\]]+)\\]");

    private static final ComparisonOperator ISNULL_OP = new ComparisonOperator("=isnull=", false);

    /* Subset of operators that map directly to Elide operators */
    private static final Map<ComparisonOperator, Operator> OPERATOR_MAP =
            ImmutableMap.<ComparisonOperator, Operator>builder()
                    .put(RSQLOperators.IN, Operator.IN)
                    .put(RSQLOperators.NOT_IN, Operator.NOT)
                    .put(RSQLOperators.LESS_THAN, Operator.LT)
                    .put(RSQLOperators.GREATER_THAN, Operator.GT)
                    .put(RSQLOperators.GREATER_THAN_OR_EQUAL, Operator.GE)
                    .put(RSQLOperators.LESS_THAN_OR_EQUAL, Operator.LE)
                    .build();

    private final RSQLParser parser;
    private final EntityDictionary dictionary;

    public RSQLFilterDialect(EntityDictionary dictionary) {
        parser = new RSQLParser(getDefaultOperatorsWithIsnull());
        this. dictionary = dictionary;
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
            throw new ParseException("There can only be a single filter query parameter");
        }

        MultivaluedMap.Entry<String, List<String>> entry = filterParams.entrySet().iterator().next();
        String queryParamName = entry.getKey();

        if (!queryParamName.equals("filter")) {
            throw new ParseException("Invalid query parameter: " + queryParamName);
        }
        List<String> queryParamValues = entry.getValue();

        if (queryParamValues.size() != 1) {
            throw new ParseException("There can only be a single filter query parameter");
        }

        String queryParamValue = queryParamValues.get(0);

        /*
         * Extract the last collection in the URL.
         */
        path = Paths.get(path).normalize().toString().replace(File.separatorChar, '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] pathComponents = path.split("/");
        String lastPathComponent = "";
        if (pathComponents.length > 0) {
            lastPathComponent = pathComponents[pathComponents.length - 1];
        }

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
            RSQL2ExpressionFilterVisitor visitor = new RSQL2ExpressionFilterVisitor(true);
            FilterExpression filterExpression = ast.accept(visitor, entityType);
            return filterExpression;
        } catch (RSQLParserException e) {
            throw new ParseException(e.getMessage());
        }
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String>
            filterParams) throws ParseException {

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

                String expressionText = paramValues.get(0);

                Class entityType = dictionary.getEntityClass(typeName);

                if (entityType == null) {
                    throw new ParseException("Invalid query parameter: " + paramName);
                }

                try {
                    Node ast = parser.parse(expressionText);
                    RSQL2ExpressionFilterVisitor visitor = new RSQL2ExpressionFilterVisitor(false);
                    FilterExpression filterExpression = ast.accept(visitor, entityType);
                    expressionByType.put(typeName, filterExpression);
                } catch (RSQLParserException e) {
                    throw new ParseException(e.getMessage());
                }
            } else {
                throw new ParseException("Invalid query parameter: " + paramName);
            }
        }
        return expressionByType;
    }

    /**
     * Visitor which converts RSQL abstract syntax tree into an Elide filter expression.
     */
    public class RSQL2ExpressionFilterVisitor implements RSQLVisitor<FilterExpression, Class> {

        /**
         * Allows base RSQLParseException to carry a parameterized message.
         */
        public class RSQLParseException extends RSQLParserException {
            String message;
            RSQLParseException(String message) {
                super(new Throwable() { });
                this.message = message;
            }
            @Override
            public String getMessage() {
                return message;
            }
        }

        @Getter
        boolean allowNestedAssociations = false;

        public RSQL2ExpressionFilterVisitor(boolean allowNestedAssociations) {
            this.allowNestedAssociations = allowNestedAssociations;
        }

        public List<FilterPredicate.PathElement> buildPath(Class rootEntityType, String selector) {
            String[] associationNames = selector.split("\\.");

            List<FilterPredicate.PathElement> path = new ArrayList<>();
            Class entityType = rootEntityType;

            for (String associationName : associationNames) {
                String typeName = dictionary.getJsonAliasFor(entityType);

                Class fieldType = dictionary.getParameterizedType(entityType, associationName);

                if (fieldType == null) {
                    throw new RSQLParseException(
                            String.format("No such association %s for type %s", associationName, typeName));
                }

                FilterPredicate.PathElement pathElement = new FilterPredicate.PathElement(
                        entityType,
                        typeName,
                        fieldType,
                        associationName);
                path.add(pathElement);

                entityType = fieldType;
            }
            return path;
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

            List<FilterPredicate.PathElement> path = buildPath(entityType, relationship);


            if (path.size() > 1 && !allowNestedAssociations) {
                throw new RSQLParseException(String.format("No such association %s", relationship));
            }

            //handles '=isnull=' op before coerce arguments
            if (op.equals(ISNULL_OP)) {
                return buildIsNullOperator(path, arguments);
            }

            Class<?> relationshipType = path.get(path.size() - 1).getFieldType();

            //Coerce arguments to their correct types
            List<Object> values = arguments
                    .stream()
                    .map((argument) -> (Object) CoerceUtil.coerce(argument, relationshipType))
                    .collect(Collectors.toList());

            if (op.equals(RSQLOperators.EQUAL)) {
                String argument = arguments.get(0);
                if (argument.startsWith("*") && argument.endsWith("*") && argument.length() > 2) {
                    argument = argument.substring(1, argument.length() - 1);
                    return new FilterPredicate(path, Operator.INFIX_CASE_INSENSITIVE,
                            Collections.singletonList(argument));
                } else if (argument.startsWith("*") && argument.length() > 1) {
                    argument = argument.substring(1, argument.length());
                    return new FilterPredicate(path, Operator.POSTFIX_CASE_INSENSITIVE,
                            Collections.singletonList(argument));
                } else if (argument.endsWith("*") && argument.length() > 1) {
                    argument = argument.substring(0, argument.length() - 1);
                    return new FilterPredicate(path, Operator.PREFIX_CASE_INSENSITIVE,
                            Collections.singletonList(argument));
                } else {
                    return new FilterPredicate(path, Operator.IN, values);
                }
            } else if (op.equals(RSQLOperators.NOT_EQUAL)) {
                String argument = arguments.get(0);
                if (argument.startsWith("*") && argument.endsWith("*")) {
                    argument = argument.substring(1, argument.length() - 1);
                    return new NotFilterExpression(
                            new FilterPredicate(path, Operator.INFIX_CASE_INSENSITIVE,
                                    Collections.singletonList(argument)));
                } else if (argument.startsWith("*")) {
                    argument = argument.substring(1, argument.length());
                    return new NotFilterExpression(
                            new FilterPredicate(path, Operator.POSTFIX_CASE_INSENSITIVE,
                                    Collections.singletonList(argument)));
                } else if (argument.endsWith("*")) {
                    argument = argument.substring(0, argument.length() - 1);
                    return new NotFilterExpression(
                            new FilterPredicate(path, Operator.PREFIX_CASE_INSENSITIVE,
                                    Collections.singletonList(argument)));
                } else {
                    return new NotFilterExpression(new FilterPredicate(path, Operator.IN, values));
                }
            } else if (OPERATOR_MAP.containsKey(op)) {
                return new FilterPredicate(path, OPERATOR_MAP.get(op), values);
            }

            throw new RSQLParseException(String.format("Invalid Operator %s", op.getSymbol()));
        }

        /**
         * Returns Predicate for '=isnull=' case depending on its arguments.
         *
         * NOTE: Filter Expression builder specially for '=isnull=' case.
         *
         * @return Returns Predicate for '=isnull=' case depending on its arguments.
         */
        private FilterExpression buildIsNullOperator(List<FilterPredicate.PathElement> path, List<String> arguments) {
            Operator elideOP;
            try {
                boolean argBool = (boolean) CoerceUtil.coerce(arguments.get(0), boolean.class);
                if (argBool) {
                    elideOP = Operator.ISNULL;
                } else {
                    elideOP = Operator.NOTNULL;
                }
                return new FilterPredicate(path, elideOP);
            } catch (InvalidValueException e) {
                throw new RSQLParseException(String.format("Invalid value for operator =isnull="));
            }
        }
    }
}
