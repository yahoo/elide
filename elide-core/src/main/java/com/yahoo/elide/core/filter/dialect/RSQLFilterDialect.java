/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static com.yahoo.elide.core.dictionary.EntityDictionary.REGULAR_ID_NAME;
import static com.yahoo.elide.core.request.Argument.ARGUMENTS_PATTERN;
import static com.yahoo.elide.core.request.Argument.getArgumentsFromString;
import static com.yahoo.elide.core.type.ClassType.COLLECTION_TYPE;
import static com.yahoo.elide.core.type.ClassType.NUMBER_TYPE;
import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static com.yahoo.elide.core.utils.TypeHelper.isPrimitiveNumberType;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.graphql.FilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.filter.predicates.IsEmptyPredicate;
import com.yahoo.elide.core.filter.predicates.IsNullPredicate;
import com.yahoo.elide.core.filter.predicates.NotEmptyPredicate;
import com.yahoo.elide.core.filter.predicates.NotNullPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Builder;
import lombok.NonNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
public class RSQLFilterDialect implements FilterDialect, SubqueryFilterDialect, JoinFilterDialect {
    private static final String SINGLE_PARAMETER_ONLY = "There can only be a single filter query parameter";
    private static final String INVALID_QUERY_PARAMETER = "Invalid query parameter: ";
    private static final Pattern TYPED_FILTER_PATTERN = Pattern.compile("filter\\[([^\\]]+)\\]");
    // field name followed by zero or more filter arguments
    // eg: name, orderDate[grain:month] , title[foo:bar][blah:Encoded+Value]
    private static final Pattern FILTER_SELECTOR_PATTERN = Pattern.compile("(\\w+)(" + ARGUMENTS_PATTERN + ")*$");
    private static final ComparisonOperator INI = new ComparisonOperator("=ini=", true);
    private static final ComparisonOperator NOT_INI = new ComparisonOperator("=outi=", true);
    private static final ComparisonOperator ISNULL_OP = new ComparisonOperator("=isnull=", false);
    private static final ComparisonOperator ISEMPTY_OP = new ComparisonOperator("=isempty=", false);
    private static final ComparisonOperator HASMEMBER_OP = new ComparisonOperator("=hasmember=", false);
    private static final ComparisonOperator HASNOMEMBER_OP = new ComparisonOperator("=hasnomember=", false);
    private static final ComparisonOperator BETWEEN_OP = new ComparisonOperator("=between=", true);
    private static final ComparisonOperator NOTBETWEEN_OP = new ComparisonOperator("=notbetween=", true);

    /* Subset of operators that map directly to Elide operators */
    private static final Map<ComparisonOperator, Operator> OPERATOR_MAP =
            ImmutableMap.<ComparisonOperator, Operator>builder()
                    .put(RSQLOperators.LESS_THAN, Operator.LT)
                    .put(RSQLOperators.GREATER_THAN, Operator.GT)
                    .put(RSQLOperators.GREATER_THAN_OR_EQUAL, Operator.GE)
                    .put(RSQLOperators.LESS_THAN_OR_EQUAL, Operator.LE)
                    .put(HASMEMBER_OP, Operator.HASMEMBER)
                    .put(HASNOMEMBER_OP, Operator.HASNOMEMBER)
                    .put(BETWEEN_OP, Operator.BETWEEN)
                    .put(NOTBETWEEN_OP, Operator.NOTBETWEEN)
                    .build();


    private final RSQLParser parser;

    @NonNull
    private final EntityDictionary dictionary;
    private final CaseSensitivityStrategy caseSensitivityStrategy;
    private final Boolean addDefaultArguments;

    @Builder
    public RSQLFilterDialect(EntityDictionary dictionary,
                             CaseSensitivityStrategy caseSensitivityStrategy,
                             Boolean addDefaultArguments) {
        parser = new RSQLParser(getDefaultOperatorsWithIsnull());
        this.dictionary = dictionary;
        if (caseSensitivityStrategy == null) {
            this.caseSensitivityStrategy = new CaseSensitivityStrategy.UseColumnCollation();
        } else {
            this.caseSensitivityStrategy = caseSensitivityStrategy;
        }

        if (addDefaultArguments == null) {
            this.addDefaultArguments = true;
        } else {
            this.addDefaultArguments = addDefaultArguments;
        }
    }

    //add rsql isnull op to the default ops
    public static final Set<ComparisonOperator> getDefaultOperatorsWithIsnull() {
        Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
        operators.add(INI);
        operators.add(NOT_INI);
        operators.add(ISNULL_OP);
        operators.add(ISEMPTY_OP);
        operators.add(HASMEMBER_OP);
        operators.add(HASNOMEMBER_OP);
        operators.add(BETWEEN_OP);
        operators.add(NOTBETWEEN_OP);
        return operators;
    }

    @Override
    public FilterExpression parse(Type<?> entityClass,
                                  Set<Attribute> attributes,
                                  String filterText,
                                  String apiVersion)
            throws ParseException {
        return parseFilterExpression(filterText, entityClass, true, true, attributes);
    }

    @Override
    public FilterExpression parseGlobalExpression(String path, MultivaluedMap<String, String> filterParams,
                                                  String apiVersion)
            throws ParseException {
        if (filterParams.size() != 1) {
            throw new ParseException(SINGLE_PARAMETER_ONLY);
        }

        MultivaluedMap.Entry<String, List<String>> entry = CollectionUtils.get(filterParams, 0);
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
        Type entityType = dictionary.getEntityClass(lastPathComponent, apiVersion);
        if (entityType == null) {
            throw new ParseException("No such collection: " + lastPathComponent);
        }

        return parseFilterExpression(queryParamValue, entityType, true);
    }

    @Override
    public Map<String, FilterExpression> parseTypedExpression(String path, MultivaluedMap<String, String> filterParams,
                                                              String apiVersion)
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

                Type entityType = dictionary.getEntityClass(typeName, apiVersion);
                if (entityType == null) {
                    throw new ParseException(INVALID_QUERY_PARAMETER + paramName);
                }

                String expressionText = paramValues.get(0);

                FilterExpression filterExpression = parseFilterExpression(expressionText, entityType, true);
                expressionByType.put(typeName, filterExpression);
            } else {
                throw new ParseException(INVALID_QUERY_PARAMETER + paramName);
            }
        }
        return expressionByType;
    }

    /**
     * Parses a RSQL string into an Elide FilterExpression.
     * @param expressionText the RSQL string
     * @param entityType The type associated with the predicate
     * @param allowNestedToManyAssociations Whether or not to reject nested filter paths.
     * @return An elide FilterExpression abstract syntax tree
     * @throws ParseException
     */
    public FilterExpression parseFilterExpression(String expressionText,
                                                  Type<?> entityType,
                                                  boolean allowNestedToManyAssociations) throws ParseException {
        return parseFilterExpression(expressionText, entityType, true, allowNestedToManyAssociations);
    }

    /**
     * Parses a RSQL string into an Elide FilterExpression.
     * @param expressionText the RSQL string
     * @param entityType The type associated with the predicate
     * @param coerceValues Convert values into their underlying type.
     * @param allowNestedToManyAssociations Whether or not to reject nested filter paths.
     * @return An elide FilterExpression abstract syntax tree
     * @throws ParseException
     */
    public FilterExpression parseFilterExpression(String expressionText,
                                                  Type<?> entityType,
                                                  boolean coerceValues,
                                                  boolean allowNestedToManyAssociations) throws ParseException {
        return parseFilterExpression(expressionText, entityType, coerceValues,
                allowNestedToManyAssociations, Collections.emptySet());

    }

    /**
     * Parses a RSQL string into an Elide FilterExpression.
     * @param expressionText the RSQL string
     * @param entityType The type associated with the predicate
     * @param coerceValues Convert values into their underlying type.
     * @param allowNestedToManyAssociations Whether or not to reject nested filter paths.
     * @param attributes the set of model attributes being requested.
     * @return An elide FilterExpression abstract syntax tree
     * @throws ParseException
     */
    public FilterExpression parseFilterExpression(String expressionText,
                                                  Type<?> entityType,
                                                  boolean coerceValues,
                                                  boolean allowNestedToManyAssociations,
                                                  Set<Attribute> attributes) throws ParseException {
        try {
            Node ast = parser.parse(expressionText);
            RSQL2FilterExpressionVisitor visitor = new RSQL2FilterExpressionVisitor(allowNestedToManyAssociations,
                    coerceValues, attributes);
            return ast.accept(visitor, entityType);
        } catch (RSQLParserException e) {
            throw new ParseException(e.getMessage());
        }
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
    public class RSQL2FilterExpressionVisitor implements RSQLVisitor<FilterExpression, Type> {
        private boolean allowNestedToManyAssociations = false;
        private boolean coerceValues = true;
        private Set<Attribute> attributes;

        public RSQL2FilterExpressionVisitor(boolean allowNestedToManyAssociations) {
            this(allowNestedToManyAssociations, true, Collections.emptySet());
        }

        public RSQL2FilterExpressionVisitor(boolean allowNestedToManyAssociations,
                                            boolean coerceValues, Set<Attribute> attributes) {
            this.allowNestedToManyAssociations = allowNestedToManyAssociations;
            this.coerceValues = coerceValues;
            this.attributes = attributes;
        }

        private Path buildAttribute(Type rootEntityType, String attributeName) {
            Attribute attribute = attributes.stream()
                    .filter(attr -> attr.getName().equals(attributeName) || attr.getAlias().equals(attributeName))
                    .findFirst().orElse(null);

            if (attribute != null) {
                return new Path(rootEntityType, dictionary, attribute.getName(),
                        attribute.getAlias(), attribute.getArguments());
            }
            return buildPath(rootEntityType, attributeName);
        }

        private Path buildPath(Type rootEntityType, String selector) {
            String[] associationNames = selector.split("\\.");

            List<Path.PathElement> path = new ArrayList<>();
            Type entityType = rootEntityType;

            for (String associationName : associationNames) {

                if (!FILTER_SELECTOR_PATTERN.matcher(associationName).matches()) {
                    throw new RSQLParseException("Filter expression is not in expected format at: " + associationName);
                }

                // if the association name is "id", replaced it with real id field name
                // id field name can be "id" or other string, but non-id field can't have name "id".
                if (associationName.equals(REGULAR_ID_NAME)) {
                    associationName = dictionary.getIdFieldName(entityType);
                }

                Set<Argument> arguments;
                int argsIndex = associationName.indexOf('[');
                if (argsIndex > 0) {
                    try {
                        arguments = getArgumentsFromString(associationName.substring(argsIndex));
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        throw new RSQLParseException(
                                        String.format("Filter expression is not in expected format at: %s. %s",
                                                        associationName, e.getMessage()));
                    }
                    associationName = associationName.substring(0, argsIndex);
                } else {
                    arguments = new HashSet<>();
                }

                if (addDefaultArguments) {
                    addDefaultArguments(arguments, dictionary.getAttributeArguments(entityType, associationName));
                }

                String typeName = dictionary.getJsonAliasFor(entityType);
                Type fieldType = dictionary.getParameterizedType(entityType, associationName);

                if (fieldType == null) {
                    throw new RSQLParseException(
                            String.format("No such association %s for type %s", associationName, typeName));
                }

                path.add(new Path.PathElement(entityType, fieldType, associationName, associationName, arguments));

                entityType = fieldType;
            }
            return new Path(path);
        }

        private void addDefaultArguments(Set<Argument> clientArguments, Set<ArgumentType> availableArgTypes) {

            Set<String> clientArgNames = clientArguments.stream()
                            .map(Argument::getName)
                            .collect(Collectors.toSet());

            // Check if there is any argument which has default value but not provided by client, then add it.
            availableArgTypes.stream()
                            .filter(argType -> !clientArgNames.contains(argType.getName()))
                            .filter(argType -> argType.getDefaultValue() != null)
                            .map(argType -> Argument.builder()
                                            .name(argType.getName())
                                            .value(argType.getDefaultValue())
                                            .build())
                            .forEach(clientArguments::add);
        }

        @Override
        public FilterExpression visit(AndNode node, Type entityType) {

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
        public FilterExpression visit(OrNode node, Type entityType) {

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
        public FilterExpression visit(ComparisonNode node, Type entityType) {
            ComparisonOperator op = node.getOperator();
            String relationship = node.getSelector();
            List<String> arguments = node.getArguments();

            Path path;
            // '[' means it has arguments
            // If arguments are passed in filter, it overrides the arguments provided in projection.
            if (relationship.contains(".") || relationship.contains("[")) {
                path = buildPath(entityType, relationship);
            } else {
                path = buildAttribute(entityType, relationship);

            }

            //handles '=isempty=' op before coerce arguments
            // ToMany Association is allowed if the operation is IsEmpty
            if (op.equals(ISEMPTY_OP)) {
                if (FilterPredicate.toManyInPathExceptLastPathElement(dictionary, path)) {
                    throw new RSQLParseException(
                            String.format("Invalid association %s. toMany association has to be the target collection.",
                                    relationship));
                }
                return buildIsEmptyOperator(path, arguments);
            }

            if (op.equals(HASMEMBER_OP) || op.equals(HASNOMEMBER_OP)) {
                if (FilterPredicate.toManyInPath(dictionary, path)) {
                    if (FilterPredicate.isLastPathElementAssignableFrom(dictionary, path, COLLECTION_TYPE)) {
                        throw new RSQLParseException("Invalid Path: Last Path Element cannot be a collection type");
                    }
                } else if (!FilterPredicate.isLastPathElementAssignableFrom(dictionary, path, COLLECTION_TYPE)) {
                    throw new RSQLParseException("Invalid Path: Last Path Element has to be a collection type");
                }
            }

            if (FilterPredicate.toManyInPath(dictionary, path) && !allowNestedToManyAssociations) {
                throw new RSQLParseException(String.format("Invalid association %s", relationship));
            }

            //handles '=isnull=' op before coerce arguments
            if (op.equals(ISNULL_OP)) {
                return buildIsNullOperator(path, arguments);
            }

            Type<?> relationshipType = path.lastElement()
                    .map(Path.PathElement::getFieldType)
                    .orElseThrow(() -> new IllegalStateException("Path must not be empty"));

            //Coerce arguments to their correct types
            List<Object> values = arguments.stream()
                    .map(argument ->
                            isPrimitiveNumberType(relationshipType) || NUMBER_TYPE.isAssignableFrom(relationshipType)
                                    ? argument.replace("*", "") //Support filtering on number types
                                    : argument
                    )
                    .map((argument) -> {
                            try {
                                return CoerceUtil.coerce(argument, relationshipType);
                            } catch (InvalidValueException e) {
                                if (coerceValues) {
                                    throw e;
                                }
                                return argument;
                            }
                    })
                    .collect(Collectors.toList());
            if (op.equals(RSQLOperators.EQUAL) || op.equals(RSQLOperators.IN)) {
                return equalityExpression(arguments.get(0), path, values, true);
            }
            if (op.equals(INI)) {
                return equalityExpression(arguments.get(0), path, values, false);
            }
            if (op.equals(RSQLOperators.NOT_EQUAL) || op.equals(RSQLOperators.NOT_IN)) {
                return new NotFilterExpression(equalityExpression(arguments.get(0), path, values, true));
            }
            if (op.equals(NOT_INI)) {
                return new NotFilterExpression(equalityExpression(arguments.get(0), path, values, false));
            }
            if (OPERATOR_MAP.containsKey(op)) {
                return new FilterPredicate(path, OPERATOR_MAP.get(op), values);
            }

            throw new RSQLParseException(String.format("Invalid Operator %s", op.getSymbol()));
        }

        private FilterExpression equalityExpression(String argument, Path path,
                                                    List<Object> values, boolean caseSensitive) {
            boolean startsWith = argument.startsWith("*");
            boolean endsWith = argument.endsWith("*");
            if (startsWith && endsWith && argument.length() > 2) {
                String value = argument.substring(1, argument.length() - 1);
                Operator op = caseSensitive
                        ? caseSensitivityStrategy.mapOperator(Operator.INFIX)
                        : Operator.INFIX_CASE_INSENSITIVE;
                return new FilterPredicate(path, op, Collections.singletonList(value));
            }
            if (startsWith && argument.length() > 1) {
                String value = argument.substring(1, argument.length());
                Operator op = caseSensitive
                        ? caseSensitivityStrategy.mapOperator(Operator.POSTFIX)
                        : Operator.POSTFIX_CASE_INSENSITIVE;
                return new FilterPredicate(path, op, Collections.singletonList(value));
            }
            if (endsWith && argument.length() > 1) {
                String value = argument.substring(0, argument.length() - 1);
                Operator op = caseSensitive
                        ? caseSensitivityStrategy.mapOperator(Operator.PREFIX)
                        : Operator.PREFIX_CASE_INSENSITIVE;
                return new FilterPredicate(path, op, Collections.singletonList(value));
            }

            boolean isStringLike = path.lastElement()
                    .filter(e -> e.getFieldType().isAssignableFrom(STRING_TYPE))
                    .isPresent();
            if (isStringLike) {
                Operator op = caseSensitive
                        ? caseSensitivityStrategy.mapOperator(Operator.IN)
                        : Operator.IN_INSENSITIVE;
                return new FilterPredicate(path, op, values);
            }

            return caseSensitive
                    ? new InPredicate(path, values)
                    : new InInsensitivePredicate(path, values);
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

        /**
         * Returns Predicate for '=isempty=' case depending on its arguments.
         * <p>
         * NOTE: Filter Expression builder specially for '=isempty=' case.
         *
         * @return
         */
        private FilterExpression buildIsEmptyOperator(Path path, List<String> arguments) {
            String arg = arguments.get(0);
            try {
                boolean wantsEmpty = CoerceUtil.coerce(arg, boolean.class);
                if (wantsEmpty) {
                    return new IsEmptyPredicate(path);
                }
                return new NotEmptyPredicate(path);
            } catch (InvalidValueException ignored) {
                throw new RSQLParseException(String.format("Invalid value for operator =isempty= '%s'", arg));
            }
        }
    }
}
