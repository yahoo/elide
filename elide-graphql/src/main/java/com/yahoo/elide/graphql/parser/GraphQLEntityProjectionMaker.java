/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.graphql.KeyWord.EDGES;
import static com.yahoo.elide.graphql.KeyWord.NODE;
import static com.yahoo.elide.graphql.KeyWord.PAGE_INFO;
import static com.yahoo.elide.graphql.KeyWord.PAGE_INFO_TOTAL_RECORDS;
import static com.yahoo.elide.graphql.KeyWord.SCHEMA;
import static com.yahoo.elide.graphql.KeyWord.TYPE;
import static com.yahoo.elide.graphql.KeyWord.TYPENAME;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.graphql.GraphQLNameUtils;
import com.yahoo.elide.graphql.ModelBuilder;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.EntityProjection.EntityProjectionBuilder;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Relationship;

import com.yahoo.elide.request.Sorting;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * This class converts a GraphQL query string into an Elide {@link EntityProjection} using
 * {@link #make(String)} method.
 */
@Slf4j
public class GraphQLEntityProjectionMaker {
    private final ElideSettings elideSettings;
    private final EntityDictionary entityDictionary;
    private final MultipleFilterDialect filterDialect;

    private final VariableResolver variableResolver;
    private final FragmentResolver fragmentResolver;

    private final Map<SourceLocation, Relationship> relationshipMap = new HashMap<>();
    private final Map<String, EntityProjection> rootProjections = new HashMap<>();
    private final Map<SourceLocation, Attribute> attributeMap = new HashMap<>();

    private final GraphQLNameUtils nameUtils;
    private final String apiVersion;

    /**
     * Constructor.
     *
     * @param elideSettings settings of current Elide instance
     * @param variables variables provided in the request
     * @param apiVersion The client requested API version.
     */
    public GraphQLEntityProjectionMaker(ElideSettings elideSettings, Map<String, Object> variables, String apiVersion) {
        this.elideSettings = elideSettings;
        this.entityDictionary = elideSettings.getDictionary();
        this.filterDialect = new MultipleFilterDialect(
                elideSettings.getJoinFilterDialects(),
                elideSettings.getSubqueryFilterDialects());

        this.variableResolver = new VariableResolver(variables);
        this.fragmentResolver = new FragmentResolver();
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        this.apiVersion = apiVersion;
    }

    /**
     * Constructor.
     *
     * @param elideSettings settings of current Elide instance
     */
    public GraphQLEntityProjectionMaker(ElideSettings elideSettings) {
        this(elideSettings, new HashMap<>(), NO_VERSION);
    }

    /**
     * Convert a GraphQL query string into a collection of Elide {@link EntityProjection}s.
     *
     * @param query GraphQL query
     * @return all projections in the query
     */
    public GraphQLProjectionInfo make(String query) {
        Parser parser = new Parser();
        Document parsedDocument;
        try {
            parsedDocument = parser.parseDocument(query);
        } catch (Exception e) {
            throw new InvalidEntityBodyException("Can't parse query: " + query);
        }

        // resolve fragment definitions
        fragmentResolver.addFragments(parsedDocument);

        // resolve operation definitions
        parsedDocument.getDefinitions().forEach(definition -> {
            if (definition instanceof OperationDefinition) {
                // Operations would be converted into EntityProjection tree
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                if (operationDefinition.getOperation() == OperationDefinition.Operation.SUBSCRIPTION) {
                    // TODO: support SUBSCRIPTION
                    return;
                }

                // resolve variable definitions in this operation
                variableResolver.newScope(operationDefinition);

                addRootProjection(operationDefinition.getSelectionSet());
            } else if (!(definition instanceof FragmentDefinition)) {
                throw new InvalidEntityBodyException(
                        String.format("Unsupported definition type {%s}.", definition.getClass()));
            }
        });

        return new GraphQLProjectionInfo(rootProjections, relationshipMap, attributeMap);
    }

    /**
     * Root projection would be an operation applied on an single entity class.
     * The EntityProjection tree would be constructed recursively to add all child projections.
     *
     * @param selectionSet a root-level selection set
     */
    private void addRootProjection(SelectionSet selectionSet) {
        List<Selection> selections = selectionSet.getSelections();

        selections.stream().forEach(rootSelection -> {
            if (!(rootSelection instanceof Field)) {
                throw new InvalidEntityBodyException("Entity selection must be a graphQL field.");
            }
            Field rootSelectionField = (Field) rootSelection;
            String entityName = rootSelectionField.getName();
            String aliasName = rootSelectionField.getAlias();
            if (SCHEMA.equals(entityName) || TYPE.equals(entityName)) {
                // '__schema' and '__type' would not be handled by entity projection
                return;
            }
            Class<?> entityType = entityDictionary.getEntityClass(rootSelectionField.getName(), apiVersion);
            if (entityType == null) {
                throw new InvalidEntityBodyException(String.format("Unknown entity {%s}.",
                        rootSelectionField.getName()));
            }


            String keyName = GraphQLProjectionInfo.computeProjectionKey(aliasName, entityName);
            if (rootProjections.containsKey(keyName)) {
                throw  new InvalidEntityBodyException(
                        String.format("Found two root level query for Entity {%s} with same alias name",
                                entityName));
            }
            rootProjections.put(keyName,
                    createProjection(entityType, rootSelectionField));
        });

    }

    /**
     * Construct an {@link EntityProjection} from a GraphQL {@link Field} for an entity type.
     *
     * @param entityType type of entity to be projected
     * @param entityField graphQL field definition
     * @return constructed {@link EntityProjection}
     */
    private EntityProjection createProjection(Class<?> entityType, Field entityField) {
        final EntityProjectionBuilder projectionBuilder = EntityProjection.builder()
                .type(entityType)
                .pagination(PaginationImpl.getDefaultPagination(entityType, elideSettings));

        entityField.getSelectionSet().getSelections().forEach(selection -> addSelection(selection, projectionBuilder));
        entityField.getArguments().forEach(argument -> addArgument(argument, projectionBuilder));

        return projectionBuilder.build();
    }

    /**
     * Add a graphQL {@link Selection} to an {@link EntityProjection}
     *
     * @param fieldSelection field/fragment to add
     * @param projectionBuilder projection that is being built
     */
    private void addSelection(Selection fieldSelection, final EntityProjectionBuilder projectionBuilder) {
        if (fieldSelection instanceof FragmentSpread) {
            addFragment((FragmentSpread) fieldSelection, projectionBuilder);
        } else if (fieldSelection instanceof Field) {
            if (EDGES.equals(((Field) fieldSelection).getName())
                    || NODE.equals(((Field) fieldSelection).getName())) {
                // if this graphql field is 'edges' or 'node', go one level deeper in the graphql document
                ((Field) fieldSelection).getSelectionSet().getSelections().forEach(
                        selection -> addSelection(selection, projectionBuilder));
            } else {
                addField((Field) fieldSelection, projectionBuilder);
            }
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unsupported selection type {%s}.", fieldSelection.getClass()));
        }
    }

    /**
     * Resolve a graphQL {@link FragmentSpread} into {@link Selection}s and add them to an {@link EntityProjection}
     *
     * @param fragment graphQL fragment
     * @param projectionBuilder projection that is being built
     */
    private void addFragment(FragmentSpread fragment, EntityProjectionBuilder projectionBuilder) {
        String fragmentName = fragment.getName();

        FragmentDefinition fragmentDefinition = fragmentResolver.get(fragmentName);

        String fragmentTypeName = fragmentDefinition.getTypeCondition().getName();
        // type name in type condition of the Fragment must match the entity projection type name
        if (fragmentTypeName.equals(nameUtils.toConnectionName(projectionBuilder.getType()))
                || fragmentTypeName.equals(nameUtils.toEdgesName(projectionBuilder.getType()))
                || fragmentTypeName.equals(nameUtils.toNodeName(projectionBuilder.getType()))) {
            fragmentDefinition.getSelectionSet().getSelections()
                    .forEach(selection -> addSelection(selection, projectionBuilder));
        }
    }

    /**
     * Add a new graphQL {@link Field} into an {@link EntityProjection}
     *
     * @param field graphQL field
     * @param projectionBuilder projection that is being built
     */
    private void addField(Field field, EntityProjectionBuilder projectionBuilder) {
        Class<?> parentType = projectionBuilder.getType();
        String fieldName = field.getName();

        // this field would either be a relationship field or an attribute field
        if (entityDictionary.getRelationshipType(parentType, fieldName) != RelationshipType.NONE) {
            // handle the case of a relationship field
            addRelationship(field, projectionBuilder);
        } else if (TYPENAME.equals(fieldName)) {
            // '__typename' would not be handled by entityProjection
            return;
        } else if (PAGE_INFO.equals(fieldName)) {
            // only 'totalRecords' needs to be added into the projection's pagination
            if (field.getSelectionSet().getSelections().stream()
                    .anyMatch(selection -> selection instanceof Field
                            && PAGE_INFO_TOTAL_RECORDS.equals(((Field) selection).getName()))) {
                addPageTotal(projectionBuilder);
            }
        } else {
            addAttributeField(field, projectionBuilder);
        }
    }

    /**
     * Create a relationship with projection and add it to the parent projection.
     *
     * @param relationshipField graphQL field for a relationship
     * @param projectionBuilder projection that is being built
     */
    private void addRelationship(Field relationshipField, EntityProjectionBuilder projectionBuilder) {
        Class<?> parentType = projectionBuilder.getType();
        String relationshipName = relationshipField.getName();
        String relationshipAlias =
                relationshipField.getAlias() == null ? relationshipName : relationshipField.getAlias();

        final Class<?> relationshipType = entityDictionary.getParameterizedType(parentType, relationshipName);

        // build new entity projection with only entity type and entity dictionary
        EntityProjection relationshipProjection = createProjection(relationshipType, relationshipField);
        Relationship relationship = Relationship.builder()
                .name(relationshipName)
                .alias(relationshipAlias)
                .projection(relationshipProjection)
                .build();

        relationshipMap.put(relationshipField.getSourceLocation(), relationship);

        // add this relationship projection to its parent projection
        projectionBuilder.relationship(relationship);
    }

    /**
     * Add an attribute to an entity projection.
     *
     * @param attributeField graphQL field for an attribute
     * @param projectionBuilder projection that is being built
     */
    private void addAttributeField(Field attributeField, EntityProjectionBuilder projectionBuilder) {
        Class<?> parentType = projectionBuilder.getType();
        String attributeName = attributeField.getName();
        String attributeAlias = attributeField.getAlias() == null ? attributeName : attributeField.getAlias();

        Class<?> attributeType = entityDictionary.getType(parentType, attributeName);
        if (attributeType != null) {
            Attribute attribute = Attribute.builder()
                    .type(attributeType)
                    .name(attributeName)
                    .alias(attributeAlias)
                    .arguments(
                            attributeField.getArguments().stream()
                                    .map(graphQLArgument -> com.yahoo.elide.request.Argument.builder()
                                            .name(graphQLArgument.getName())
                                            .value(
                                                    variableResolver.resolveValue(
                                                            graphQLArgument.getValue()))
                                            .build())
                                    .collect(Collectors.toList()))
                    .build();

            projectionBuilder.attribute(attribute);
            attributeMap.put(attributeField.getSourceLocation(), attribute);
        } else {
            throw new InvalidEntityBodyException(String.format(
                            "Unknown attribute field {%s.%s}.",
                            entityDictionary.getJsonAliasFor(projectionBuilder.getType()),
                            attributeName));
        }
    }

    /**
     * Construct Elide {@link Pagination}, {@link Sorting}, {@link Attribute} from GraphQL {@link Argument} and
     * add it to the {@link EntityProjection}.
     *
     * @param argument graphQL argument
     * @param projectionBuilder projection that is being built
     */
    private void addArgument(Argument argument, EntityProjectionBuilder projectionBuilder) {
        String argumentName = argument.getName();

        if (isPaginationArgument(argumentName)) {
            addPagination(argument, projectionBuilder);
        } else if (isSortingArgument(argumentName)) {
            addSorting(argument, projectionBuilder);
        } else if (ModelBuilder.ARGUMENT_FILTER.equals(argumentName)) {
            addFilter(argument, projectionBuilder);
        } else if (!ModelBuilder.ARGUMENT_OPERATION.equals(argumentName)
                && !(ModelBuilder.ARGUMENT_IDS.equals(argumentName))
                && !(ModelBuilder.ARGUMENT_DATA.equals(argumentName))) {
            addAttributeArgument(argument, projectionBuilder);
        }
    }

    /**
     * Returns whether or not a GraphQL argument name corresponding to a pagination argument.
     *
     * @param argumentName Name key of the GraphQL argument
     *
     * @return {@code true} if the name equals to {@link ModelBuilder#ARGUMENT_FIRST} or
     * {@link ModelBuilder#ARGUMENT_AFTER}
     */
    private static boolean isPaginationArgument(String argumentName) {
        return ModelBuilder.ARGUMENT_FIRST.equals(argumentName) || ModelBuilder.ARGUMENT_AFTER.equals(argumentName);
    }

    /**
     * Create a {@link Pagination} object from pagination GraphQL argument and attach it to the building
     * {@link EntityProjection}.
     *
     * @param argument graphQL argument
     * @param projectionBuilder projection that is being built
     */
    private void addPagination(Argument argument, EntityProjectionBuilder projectionBuilder) {
        Pagination pagination = projectionBuilder.getPagination() == null
                ? PaginationImpl.getDefaultPagination(projectionBuilder.getType(), elideSettings)
                : projectionBuilder.getPagination();

        Object argumentValue = variableResolver.resolveValue(argument.getValue());
        int value = argumentValue instanceof BigInteger
                ? ((BigInteger) argumentValue).intValue()
                : Integer.parseInt((String) argumentValue);
        if (ModelBuilder.ARGUMENT_FIRST.equals(argument.getName())) {
            pagination = new PaginationImpl(
                    projectionBuilder.getType(),
                    pagination.getOffset(),
                    value,
                    elideSettings.getDefaultPageSize(),
                    elideSettings.getDefaultPageSize(),
                    pagination.returnPageTotals(),
                    false);
        } else if (ModelBuilder.ARGUMENT_AFTER.equals(argument.getName())) {
            pagination = new PaginationImpl(
                    projectionBuilder.getType(),
                    value,
                    pagination.getLimit(),
                    elideSettings.getDefaultPageSize(),
                    elideSettings.getDefaultPageSize(),
                    pagination.returnPageTotals(),
                    false);
        }

        projectionBuilder.pagination(pagination);
    }

    /**
     * Make projection return page total records.
     * If the projection already has a pagination, use limit and offset from the existing pagination,
     * else use the default pagination vales.
     *
     * @param projectionBuilder projection that is being built
     */
    private void addPageTotal(EntityProjectionBuilder projectionBuilder) {
        PaginationImpl pagination;
        if (projectionBuilder.getPagination() == null) {
            pagination = new PaginationImpl(
                    projectionBuilder.getType(),
                    null,
                    null,
                    elideSettings.getDefaultPageSize(),
                    elideSettings.getDefaultMaxPageSize(),
                    true,
                    false);

        } else {
            pagination = new PaginationImpl(
                    projectionBuilder.getType(),
                    projectionBuilder.getPagination().getOffset(),
                    projectionBuilder.getPagination().getLimit(),
                    elideSettings.getDefaultPageSize(),
                    elideSettings.getDefaultMaxPageSize(),
                    true,
                    false);
        }
        projectionBuilder.pagination(pagination);
    }

    /**
     * Returns whether or not a GraphQL argument name corresponding to a sorting argument.
     *
     * @param argumentName Name key of the GraphQL argument
     *
     * @return {@code true} if the name equals to {@link ModelBuilder#ARGUMENT_SORT}
     */
    private static boolean isSortingArgument(String argumentName) {
        return ModelBuilder.ARGUMENT_SORT.equals(argumentName);
    }

    /**
     * Creates a {@link Sorting} object from sorting GraphQL argument value and attaches it to the entity sorted
     * according to the newly created {@link Sorting} object.
     *
     * @param argument An argument that contains the value of sorting spec
     * @param projectionBuilder projection that is being built
     */
    private void addSorting(Argument argument, EntityProjectionBuilder projectionBuilder) {
        String sortRule = (String) variableResolver.resolveValue(argument.getValue());

        try {
            Sorting sorting = SortingImpl.parseSortRule(sortRule, projectionBuilder.getType(), entityDictionary);
            projectionBuilder.sorting(sorting);
        } catch (InvalidValueException e) {
            throw new BadRequestException("Invalid sorting clause " + sortRule
                    + " for type " + entityDictionary.getJsonAliasFor(projectionBuilder.getType()));
        }

    }

    /**
     * Add a new filter expression to the entityProjection
     *
     * @param argument filter argument
     * @param projectionBuilder projection that is being built
     */
    private void addFilter(Argument argument, EntityProjectionBuilder projectionBuilder) {
        FilterExpression filter = buildFilter(
                projectionBuilder,
                entityDictionary.getJsonAliasFor(projectionBuilder.getType()),
                variableResolver.resolveValue(argument.getValue()));

        if (projectionBuilder.getFilterExpression() != null) {
            projectionBuilder.filterExpression(
                    new AndFilterExpression(projectionBuilder.getFilterExpression(), filter));
        } else {
            projectionBuilder.filterExpression(filter);
        }
    }

    /**
     * Construct a filter expression from a string
     *
     * @param typeName class type name to apply this filter
     * @param filterString Elide filter in string format
     * @return constructed filter expression
     */
    private FilterExpression buildFilter(EntityProjectionBuilder builder, String typeName, Object filterString) {
        if (!(filterString instanceof String)) {
            throw new BadRequestException("Filter of type " + typeName + " is not StringValue.");
        }

        String errorMessage = "";
        try {
            return filterDialect.parseGlobalExpression(typeName,
                    toQueryParams(Optional.empty(), filterString.toString()), apiVersion);

        } catch (ParseException e) {
            errorMessage = e.getMessage();
        }

        try {
            return filterDialect.parseTypedExpression(typeName,
                    toQueryParams(Optional.of(typeName), filterString.toString()), apiVersion).get(typeName);
        } catch (ParseException e) {
            throw new BadRequestException(errorMessage + "\n" + e.getMessage());
        }
    }

    /**
     * Convert a type name and filter string to a map that mimic query params comes from request.
     *
     * @param typeName optional model type name to apply this filter
     * @param filterStr Elide filter in string format
     * @return constructed map
     */
    private static MultivaluedHashMap<String, String> toQueryParams(Optional<String> typeName, String filterStr) {
        return new MultivaluedHashMap<String, String>() {
            {
                String filterKey = "filter";
                if (typeName.isPresent()) {
                    filterKey += "[" + typeName + "]";
                }
                put(filterKey, Arrays.asList(filterStr));
            }
        };
    }

    /**
     * Add argument for a field/relationship of an entity
     *
     * @param argument an argument which name should match a field name/alias
     * @param projectionBuilder projection that is being built
     */
    private void addAttributeArgument(Argument argument, EntityProjectionBuilder projectionBuilder) {
        String argumentName = argument.getName();
        Class<?> entityType = projectionBuilder.getType();

        Attribute existingAttribute = projectionBuilder.getAttributeByAlias(argumentName);

        com.yahoo.elide.request.Argument elideArgument = com.yahoo.elide.request.Argument.builder()
                .name(argumentName)
                .value(variableResolver.resolveValue(argument.getValue()))
                .build();

        if (existingAttribute != null) {
            // add a new argument to the existing attribute
            Attribute toAdd = Attribute.builder()
                    .type(existingAttribute.getType())
                    .name(existingAttribute.getName())
                    .alias(existingAttribute.getAlias())
                    .argument(elideArgument)
                    .build();

            projectionBuilder.attribute(toAdd);
        } else {
            Class<?> attributeType = entityDictionary.getType(entityType, argumentName);
            if (attributeType == null) {
                throw new InvalidEntityBodyException(
                        String.format("Invalid attribute field/alias for argument: {%s}.{%s}",
                                entityType,
                                argumentName)
                );
            }

            // create a new attribute if this attribute doesn't exist in the projection
            Attribute toAdd = Attribute.builder()
                    .type(attributeType)
                    .name(argumentName)
                    .alias(argumentName)
                    .argument(elideArgument)
                    .build();

            projectionBuilder.attribute(toAdd);
        }
    }
}
