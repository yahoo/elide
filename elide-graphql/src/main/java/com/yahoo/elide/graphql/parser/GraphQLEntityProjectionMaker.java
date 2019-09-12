/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import static com.yahoo.elide.graphql.KeyWord.EDGES;
import static com.yahoo.elide.graphql.KeyWord.NODE;
import static com.yahoo.elide.graphql.KeyWord.PAGE_INFO;
import static com.yahoo.elide.graphql.KeyWord.SCHEMA;
import static com.yahoo.elide.graphql.KeyWord.TYPE;
import static com.yahoo.elide.graphql.KeyWord.TYPENAME;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class converts a GraphQL query string into an Elide {@link EntityProjection} using {@link #make(String)}.
 */
@Slf4j
public class GraphQLEntityProjectionMaker {
    private final EntityDictionary entityDictionary;

    private Map<String, FragmentDefinition> fragmentMap = new HashMap<>();
    private Map<SourceLocation, Relationship> relationshipMap;
    private List<EntityProjection> rootProjections;
    private SourceLocation rootLocation;

    /**
     * Constructor.
     *
     * @param entityDictionary entity dictionary of current Elide instance
     */
    public GraphQLEntityProjectionMaker(EntityDictionary entityDictionary) {
        this.entityDictionary = entityDictionary;
    }

    /**
     * Clear cache.
     */
    private void clear() {
        fragmentMap = new HashMap<>();
        relationshipMap = new HashMap<>();
        rootProjections = new ArrayList<>();
        rootLocation = null;
    }

    /**
     * Convert a GraphQL query string into a collection of Elide {@link EntityProjection}s.
     *
     * @param query GraphQL query
     * @return all projections in the query
     */
    public GraphQLProjectionInfo make(String query) {
        clear();

        Parser parser = new Parser();
        Document parsedDocument = parser.parseDocument(query);

        // resolve fragment definitions
        List<FragmentDefinition> fragmentDefinitions = parsedDocument.getDefinitions().stream()
                .filter(definition -> definition instanceof FragmentDefinition)
                .map(definition -> (FragmentDefinition) definition)
                .collect(Collectors.toList());
        fragmentMap.putAll(FragmentResolver.resolve(fragmentDefinitions));

        // resolve operation definitions
        parsedDocument.getDefinitions().forEach(definition -> {
            if (definition instanceof OperationDefinition) {
                // Operations would be converted into EntityProjection tree
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                if (operationDefinition.getOperation() == OperationDefinition.Operation.SUBSCRIPTION) {
                    // TODO: support SUBSCRIPTION
                    return;
                }

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
                .dictionary(entityDictionary)
                .type(entityType)
                .build();

        // initialize the attribute container
        entityProjection.setAttributes(new HashSet<>());

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
        if (!fragmentMap.containsKey(fragmentName)) {
            throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", fragmentName));
        }

        FragmentDefinition fragmentDefinition = fragmentMap.get(fragmentName);

        // type name in type condition of the Fragment must match the entity projection type name
        if (parentProjection.getName().equals(fragmentDefinition.getTypeCondition().getName())) {
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

        if (TYPENAME.equals(fieldName) || PAGE_INFO.equals(fieldName)) {
            return; // '__typename' and 'pageInfo' would not be handled by entityProjection
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
                                            .value(graphQLArgument.getValue())
                                            .build()));
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unknown attribute field {%s.%s}.", parentProjection.getName(), fieldName));
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
                .value(argument.getValue())
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
