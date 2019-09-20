/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

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
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.ModelBuilder;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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
    private final List<EntityProjection> rootProjections = new ArrayList<>();

    private SourceLocation rootLocation;

    /**
     * Constructor.
     *
     * @param elideSettings settings of current Elide instance
     * @param variables variables provided in the request
     */
    public GraphQLEntityProjectionMaker(ElideSettings elideSettings, Map<String, Object> variables) {
        this.elideSettings = elideSettings;
        this.entityDictionary = elideSettings.getDictionary();
        this.filterDialect = new MultipleFilterDialect(
                elideSettings.getJoinFilterDialects(),
                elideSettings.getSubqueryFilterDialects());

        this.variableResolver = new VariableResolver(variables);
        this.fragmentResolver = new FragmentResolver();
    }

    /**
     * Constructor.
     *
     * @param elideSettings settings of current Elide instance
     */
    public GraphQLEntityProjectionMaker(ElideSettings elideSettings) {
        this(elideSettings, new HashMap<>());
    }

    /**
     * Convert a GraphQL query string into a collection of Elide {@link EntityProjection}s.
     *
     * @param query GraphQL query
     * @return all projections in the query
     */
    public GraphQLProjectionInfo make(String query) {
        Parser parser = new Parser();
        Document parsedDocument = parser.parseDocument(query);

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

        return new GraphQLProjectionInfo(rootLocation, rootProjections, relationshipMap);
    }

    /**
     * Root projection would be an operation applied on an entity class. There should be only one selection, which
     * is the entity, in the selection set. The EntityProjection tree would be constructed recursively to add all
     * child projections.
     *
     * @param selectionSet a root-level selection set
     */
    private void addRootProjection(SelectionSet selectionSet) {
        List<Selection> selections = selectionSet.getSelections();

        Selection rootSelection = selections.get(0);
        if (!(rootSelection instanceof Field)) {
            throw new InvalidEntityBodyException("Entity selection must be a graphQL field.");
        }

        String entityName = ((Field) rootSelection).getName();
        if (SCHEMA.equals(entityName) || TYPE.equals(entityName)) {
            // '__schema' and '__type' would not be handled by entity projection
            return;
        }

        Class<?> entityType = entityDictionary.getEntityClass(entityName);
        if (entityType == null) {
            throw new InvalidEntityBodyException(String.format("Unknown entity {%s}.", entityName));
        }

        rootLocation = rootSelection.getSourceLocation();
        Field fields = (Field) rootSelection;

        // merging partial graphql selections
        selections.stream().skip(1).forEach(selection -> {
            if (!(selection instanceof Field)) {
                throw new InvalidEntityBodyException("Entity selection must be a graphQL field.");
            }
            if (!entityName.equals(((Field) selection).getName())) {
                throw new InvalidEntityBodyException("Can't select multiple entities in graphQL QUERY operation.");
            }
            fields.getSelectionSet().getSelections().addAll(((Field) selection).getSelectionSet().getSelections());
        });

        rootProjections.add(createProjection(entityType, fields));
    }

    /**
     * Construct an {@link EntityProjection} from a GraphQL {@link Field} for an entity type.
     *
     * @param entityType type of entity to be projected
     * @param entityField graphQL field definition
     * @return constructed {@link EntityProjection}
     */
    private EntityProjection createProjection(Class<?> entityType, Field entityField) {
        final EntityProjection entityProjection = EntityProjection.builder()
                .type(entityType)
                .build();

        // initialize the attribute container
        entityProjection.setAttributes(new HashSet<>());
        entityField.getArguments().forEach(argument -> addArgument(argument, entityProjection));

        entityField.getSelectionSet().getSelections().forEach(selection -> addSelection(selection, entityProjection));

        return entityProjection;
    }

    /**
     * Add a graphQL {@link Selection} to an {@link EntityProjection}
     *
     * @param fieldSelection field/fragment to add
     * @param parentProjection projection that has this field/fragment
     */
    private void addSelection(Selection fieldSelection, final EntityProjection parentProjection) {
        if (fieldSelection instanceof FragmentSpread) {
            addFragment((FragmentSpread) fieldSelection, parentProjection);
        } else if (fieldSelection instanceof Field) {
            if (EDGES.equals(((Field) fieldSelection).getName())
                    || NODE.equals(((Field) fieldSelection).getName())) {
                // if this graphql field is 'edges' or 'node', go one level deeper in the graphql document
                ((Field) fieldSelection).getSelectionSet().getSelections().forEach(
                        selection -> addSelection(selection, parentProjection));
            } else {
                addField((Field) fieldSelection, parentProjection);
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
     * @param parentProjection entity projection that contains this fragment
     */
    private void addFragment(FragmentSpread fragment, EntityProjection parentProjection) {
        String fragmentName = fragment.getName();
        if (!fragmentResolver.contains(fragmentName)) {
            throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", fragmentName));
        }

        FragmentDefinition fragmentDefinition = fragmentResolver.get(fragmentName);

        // type name in type condition of the Fragment must match the entity projection type name
        if (entityDictionary.getJsonAliasFor(parentProjection.getType())
                .equals(fragmentDefinition.getTypeCondition().getName())) {
            fragmentDefinition.getSelectionSet().getSelections()
                    .forEach(selection -> addSelection(selection, parentProjection));
        }
    }

    /**
     * Add a new graphQL {@link Field} into an {@link EntityProjection}
     *
     * @param field graphQL field
     * @param parentProjection entity projection that contains this field
     */
    private void addField(Field field, EntityProjection parentProjection) {
        Class<?> parentType = parentProjection.getType();
        String fieldName = field.getName();

        // this field would either be a relationship field or an attribute field
        if (entityDictionary.getRelationshipType(parentType, fieldName) != RelationshipType.NONE) {
            // handle the case of a relationship field
            final Class<?> relationshipType = entityDictionary.getParameterizedType(parentType, fieldName);

            // build new entity projection with only entity type and entity dictionary
            EntityProjection relationshipProjection = createProjection(relationshipType, field);
            Relationship relationship = Relationship.builder()
                    .name(fieldName)
                    .alias(field.getAlias())
                    .projection(relationshipProjection)
                    .build();

            relationshipMap.put(field.getSourceLocation(), relationship);

            // add this relationship projection to its parent projection
            parentProjection.getRelationships().add(relationship);

            return;
        }

        if (TYPENAME.equals(fieldName)) {
            return; // '__typename' would not be handled by entityProjection
        }
        if (PAGE_INFO.equals(fieldName)) {
            // only 'totalRecords' needs to be added into the projection's pagination
            if (field.getSelectionSet().getSelections().stream()
                    .anyMatch(selection -> selection instanceof Field
                            && PAGE_INFO_TOTAL_RECORDS.equals(((Field) selection).getName()))) {
                addPageTotal(parentProjection);
            }
            return;
        }

        Class<?> attributeType = entityDictionary.getType(parentType, fieldName);
        if (attributeType != null) {
            Set<Attribute> attributes = parentProjection.getAttributes();
            Attribute matched = parentProjection.getAttributeByName(fieldName);

            if (matched == null) {
                // this is a new attribute, add it
                attributes.add(Attribute.builder().type(attributeType).name(fieldName).build());

                // update mutated attributes
                parentProjection.setAttributes(attributes);
            }

            // arguments for field should be added by projection make, elide-core would handle arguments on entities,
            // including relationship entities
            field.getArguments().forEach(
                    graphQLArgument ->
                            parentProjection.getAttributeByName(fieldName).getArguments().add(
                                    com.yahoo.elide.request.Argument.builder()
                                            .name(graphQLArgument.getName())
                                            .value(variableResolver.resolveValue(graphQLArgument.getValue()))
                                            .build()));
        } else {
            throw new InvalidEntityBodyException(
                    String.format(
                            "Unknown attribute field {%s.%s}.",
                            entityDictionary.getJsonAliasFor(parentProjection.getType()),
                            fieldName));
        }
    }

    /**
     * Construct Elide {@link Pagination}, {@link Sorting}, {@link Attribute} from GraphQL {@link Argument} and
     * add it to the {@link EntityProjection}.
     *
     * @param argument graphQL argument
     * @param entityProjection projection that has this argument
     */
    private void addArgument(Argument argument, EntityProjection entityProjection) {
        String argumentName = argument.getName();

        if (isPaginationArgument(argumentName)) {
            addPagination(argument, entityProjection);
        } else if (isSortingArgument(argumentName)) {
            addSorting(argument, entityProjection);
        } else if (ModelBuilder.ARGUMENT_FILTER.equals(argumentName)) {
            addFilter(argument, entityProjection);
        } else if (!ModelBuilder.ARGUMENT_OPERATION.equals(argumentName)
                && !(ModelBuilder.ARGUMENT_IDS.equals(argumentName))
                && !(ModelBuilder.ARGUMENT_DATA.equals(argumentName))) {
            addAttributeArgument(argument, entityProjection);
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
     * Creates a {@link Pagination} object from pagination GraphQL argument and attaches it to the
     * {@link EntityProjection}.
     *
     * @param argument graphQL argument
     * @param entityProjection projection that has this argument
     */
    private void addPagination(Argument argument, EntityProjection entityProjection) {
        Pagination pagination = entityProjection.getPagination() == null
                ? Pagination.getDefaultPagination(elideSettings)
                : entityProjection.getPagination();

        Object argumentValue = variableResolver.resolveValue(argument.getValue());
        int value = argumentValue instanceof BigInteger
                ? ((BigInteger) argumentValue).intValue()
                : Integer.parseInt((String) argumentValue);
        if (ModelBuilder.ARGUMENT_FIRST.equals(argument.getName())) {
            pagination.setLimit(value);
        } else if (ModelBuilder.ARGUMENT_AFTER.equals(argument.getName())) {
            pagination.setOffset(value);
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unrecognized pagination argument '%s'", argument.getName()));
        }

        entityProjection.setPagination(pagination);
    }

    /**
     * Make projection return page total records.
     * If the projection already has a pagination, use limit and offset from the existing pagination,
     * else use the default pagination vales.
     *
     * @param entityProjection entityProjection to modify
     */
    private void addPageTotal(EntityProjection entityProjection) {
        if (entityProjection.getPagination() == null) {
            Optional<Pagination> pagination = Pagination.fromOffsetAndFirst(
                    Optional.empty(),
                    Optional.empty(),
                    true,
                    elideSettings
            );
            pagination.ifPresent(entityProjection::setPagination);
        } else {
            Optional<Pagination> pagination = Pagination.fromOffsetAndFirst(
                    Optional.of(String.valueOf(entityProjection.getPagination().getLimit())),
                    Optional.of(String.valueOf(entityProjection.getPagination().getOffset())),
                    true,
                    elideSettings
            );
            pagination.ifPresent(entityProjection::setPagination);
        }
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
     */
    private void addSorting(Argument argument, EntityProjection entityProjection) {
        String sortRule = (String) variableResolver.resolveValue(argument.getValue());
        Sorting sorting = Sorting.parseSortRule(sortRule);

        // validate sorting rule
        try {
            sorting.getValidSortingRules(entityProjection.getType(), entityDictionary);
        } catch (InvalidValueException e) {
            throw new BadRequestException("Invalid sorting clause " + sortRule
                    + " for type " + entityDictionary.getJsonAliasFor(entityProjection.getType()));
        }

        entityProjection.setSorting(sorting);
    }

    /**
     * Add a new filter expression to the entityProjection
     *
     * @param argument filter argument
     * @param entityProjection entityProjection to modify
     */
    private void addFilter(Argument argument, EntityProjection entityProjection) {
        FilterExpression filter = buildFilter(
                entityDictionary.getJsonAliasFor(entityProjection.getType()),
                variableResolver.resolveValue(argument.getValue()));

        if (entityProjection.getFilterExpression() != null) {
            entityProjection.setFilterExpression(
                    new AndFilterExpression(entityProjection.getFilterExpression(), filter));
        } else {
            entityProjection.setFilterExpression(filter);
        }
    }

    /**
     * Construct a filter expression from a string
     *
     * @param typeName class type name to apply this filter
     * @param filterString Elide filter in string format
     * @return constructed filter expression
     */
    private FilterExpression buildFilter(String typeName, Object filterString) {
        if (!(filterString instanceof String)) {
            throw new BadRequestException("Filter of type " + typeName + " is not StringValue.");
        }
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>() {
            {
                put("filter[" + typeName + "]", Collections.singletonList((String) filterString));
            }
        };
        try {
            return filterDialect.parseTypedExpression(typeName, queryParams).get(typeName);
        } catch (ParseException e) {
            log.debug("Filter parse exception caught", e);
            throw new InvalidPredicateException("Could not parse filter " + filterString + " for type: " + typeName);
        }
    }

    /**
     * Add argument for a field/relationship of an entity
     *
     * @param argument argument which a field/relationship name
     * @param entityProjection projection for the target entity
     */
    private void addAttributeArgument(Argument argument, EntityProjection entityProjection) {
        String argumentName = argument.getName();
        Class<?> entityType = entityProjection.getType();

        Attribute argumentAttribute = entityProjection.getAttributeByName(argumentName);
        com.yahoo.elide.request.Argument elideArgument = com.yahoo.elide.request.Argument.builder()
                .name(argumentName)
                .value(variableResolver.resolveValue(argument.getValue()))
                .build();

        if (argumentAttribute == null) {
            argumentAttribute = Attribute.builder()
                    .type(entityDictionary.getType(entityType, argumentName))
                    .name(argumentName)
                    .argument(elideArgument)
                    .build();

            Set<Attribute> attributes = entityProjection.getAttributes();
            attributes.add(argumentAttribute);

            entityProjection.setAttributes(attributes);
        } else {
            argumentAttribute.getArguments().add(elideArgument);
        }
    }
}
