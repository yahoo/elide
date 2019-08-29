package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GraphQLEntityProjectionMaker {
    private final EntityDictionary entityDictionary;
    private final Map<String, List<EntityProjection>> fragmentToProjections = new HashMap<>();
    private final Map<String, FragmentDefinition> fragmentDefinitions = new HashMap<>();

    public GraphQLEntityProjectionMaker(EntityDictionary entityDictionary) {
        this.entityDictionary = entityDictionary;
    }

    private final List<EntityProjection> rootProjections = new ArrayList<>();

    public Collection<EntityProjection> make(String query) {
        Parser parser = new Parser();
        Document parsedDocument = parser.parseDocument(query);

        parsedDocument.getDefinitions().forEach(definition -> {
            if (definition instanceof OperationDefinition) {
                // Operations would be converted into EntityProjection tree
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                if (operationDefinition.getOperation() != OperationDefinition.Operation.QUERY) {
                    // TODO: support MUTATION and SUBSCRIPTION
                    return;
                }

                addRootProjections(operationDefinition.getSelectionSet());
            } else if (definition instanceof FragmentDefinition) {
                // FragmentDefinition should be stored first and then inserted into EntityProjections that use it
                FragmentDefinition fragmentDefinition = (FragmentDefinition) definition;
                fragmentDefinitions.put(fragmentDefinition.getName(), fragmentDefinition);
            } else {
                throw new InvalidEntityBodyException(
                        String.format("Unsupported definition type {%s}.", definition.getClass()));
            }
        });

        // make sure the fragment definition would not form a loop
        final Set<String> fragmentNames = new HashSet<>();
        fragmentDefinitions.values()
                .forEach(fragmentDefinition -> checkFragmentLoop(fragmentDefinition, fragmentNames));

        // as there is no loop in the fragment definition, this loop would eventually end
        while(!fragmentToProjections.isEmpty()) {
            fragmentDefinitions.forEach((fragmentName, fragmentDefinition) -> {
                if (fragmentToProjections.containsKey(fragmentName)) {
                    List<Selection> fragmentFields = fragmentDefinition.getSelectionSet().getSelections();
                    List<EntityProjection> projections = fragmentToProjections.remove(fragmentName);

                    // the entity type of the projection must be the same type in the fragment type condition
                    String fragmentTypeName = fragmentDefinition.getTypeCondition().getName();
                    List<String> failedTypeCondition = projections.stream()
                            .map(EntityProjection::getName)
                            .filter(name -> !Objects.equals(name, fragmentTypeName))
                            .collect(Collectors.toList());
                    if (failedTypeCondition.size() != 0) {
                        throw new InvalidEntityBodyException("Projections failed fragment type condition check {" +
                                String.join(",", failedTypeCondition) + "} with fragmentName: " +
                                fragmentName + ".");
                    }

                    // add fields into projections, if new fragments are referenced, they would be added into the map
                    projections.forEach(projection -> fragmentFields.forEach(field -> addField(field, projection)));
                }
            });
        }

        return rootProjections;
    }

    /**
     * Root projection would be an operation applied on an entity class. There should be only one selection, which
     * is the entity, in the selection set. The EntityProjection tree would be constructed recursively to add all
     * child projections.
     */
    private void addRootProjections(SelectionSet selectionSet) {
        List<Selection> selections = selectionSet.getSelections();

        if (selections.size() != 1) {
            throw new InvalidEntityBodyException("Can't select multiple entities in graphQL QUERY operation.");
        }

        Selection entitySelection = selections.get(0);
        if (!(entitySelection instanceof Field)) {
            throw new InvalidEntityBodyException("Entity selection must be a graphQL field.");
        }

        Field entityField = (Field) entitySelection;
        String entityName = entityField.getName();
        Class<?> entityType = entityDictionary.getEntityClass(entityName);
        if (entityType == null) {
            throw new InvalidEntityBodyException(String.format("Unknown entity {%s}.", entityName));
        }

        rootProjections.add(createProjection(entityType, ((Field) entitySelection).getSelectionSet()));
    }

    /**
     * Create an {@link EntityProjection} with entity type and Elide 'edges' container.
     * This method would call {@link #addField(Selection, EntityProjection)} which calls this method recursively
     * to build child projections.
     *
     * @param entityType entity type of new projection
     * @param edges Elide 'edges' container
     * @return constructed {@link EntityProjection}
     */
    private EntityProjection createProjection(Class<?> entityType, SelectionSet edges) {
        final EntityProjection entityProjection = EntityProjection.builder()
                .dictionary(entityDictionary)
                .type(entityType)
                .build();

        SelectionSet node = addEdges(edges, entityProjection);
        SelectionSet fields = addNode(node, entityProjection);
        fields.getSelections().forEach(fieldSelection -> addField(fieldSelection, entityProjection));

        return entityProjection;
    }

    /**
     * Get information about an entity projection from Elide 'edges' container and get the Elide 'node' container
     *
     * @param edges Elide 'edges' container
     * @param entityProjection {@link EntityProjection} that links with this container
     * @return Elide 'node' container
     */
    private static SelectionSet addEdges(SelectionSet edges, EntityProjection entityProjection) {
        if (edges.getSelections().size() != 1) {
            throw new InvalidEntityBodyException("Entity selection must have one 'edges' graphQL field node.");
        }

        Selection edgesSelection = edges.getSelections().get(0);
        if (!(edgesSelection instanceof Field)) {
            throw new InvalidEntityBodyException("Edges selection must be a graphQL field.");
        }

        return ((Field) edgesSelection).getSelectionSet();
    }

    /**
     * Get information about an entity projection from Elide 'node' container and get fields from the container
     *
     * @param node Elide 'node' container
     * @param entityProjection {@link EntityProjection} that links with this container
     * @return Fields in the 'node' container
     */
    private static SelectionSet addNode(SelectionSet node, EntityProjection entityProjection) {
        if (node.getSelections().size() != 1) {
            throw new InvalidEntityBodyException("Edges selection must have one 'node' graphQL field node.");
        }

        Selection nodeSelection = node.getSelections().get(0);
        if (!(nodeSelection instanceof Field)) {
            throw new InvalidEntityBodyException("Node selection must be a graphQL field.");
        }

        return ((Field) nodeSelection).getSelectionSet();
    }

    /**
     * Add a {@link Selection} that represents a field/fragment to an {@link EntityProjection}
     *
     * @param fieldSelection field/fragment to add
     * @param parentProjection projection that has this field/fragment
     */
    private void addField(Selection fieldSelection, final EntityProjection parentProjection) {
        if (fieldSelection instanceof FragmentSpread) {
            String fragmentName = ((FragmentSpread) fieldSelection).getName();
            if (fragmentToProjections.containsKey(fragmentName)) {
                fragmentToProjections.get(fragmentName).add(parentProjection);
            } else {
                fragmentToProjections.put(fragmentName, Collections.singletonList(parentProjection));
            }
        } else if (fieldSelection instanceof Field) {
            Field field = (Field) fieldSelection;
            String fieldName = field.getName();
            Class<?> parentType = parentProjection.getType();

            // this field would either be a relationship field or an attribute field
            if (entityDictionary.getRelationshipType(parentType, fieldName) != RelationshipType.NONE) {
                // handle the case of a relationship field
                final Class<?> relationshipType =
                        entityDictionary.getParameterizedType(parentType, fieldName) == null
                                ? entityDictionary.getType(parentType, fieldName)
                                : entityDictionary.getParameterizedType(parentType, fieldName);

                // build new entity projection with only entity type and entity dictionary
                EntityProjection relationshipProjection = createProjection(
                        relationshipType,
                        field.getSelectionSet()
                );

                // add this relationship projection to its parent projection
                parentProjection.getRelationships().put(fieldName, relationshipProjection);

                return;
            }

            Class<?> attributeType = entityDictionary.getType(parentType, fieldName);
            if (attributeType != null) {
                Attribute attribute = Attribute.builder().type(attributeType).name(fieldName).build();
                parentProjection.getAttributes().add(attribute);
            } else {
                throw new InvalidEntityBodyException(
                        String.format("Unknown field {%s.%s}.", parentProjection.getName(), fieldName));
            }
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unsupported selection type {%s}.", fieldSelection.getClass()));
        }
    }

    /**
     * Recursive DFS check to validate that there is not reference loop in a fragment.
     *
     * @param fragmentDefinition fragment to be checked
     * @param fragmentNames fragment names appear in the current check path
     */
    private void checkFragmentLoop(FragmentDefinition fragmentDefinition, Set<String> fragmentNames) {
        String fragmentName = fragmentDefinition.getName();
        if (fragmentNames.contains(fragmentName)) {
            throw new InvalidEntityBodyException("There is a fragment definition loop in: {"
                    + String.join(",", fragmentNames) + "} with " + fragmentName + " duplicated.");
        }

        fragmentNames.add(fragmentName);

        fragmentDefinition.getSelectionSet().getSelections().stream()
                .filter(selection -> selection instanceof FragmentSpread)
                .map(fragment -> ((FragmentSpread) fragment).getName())
                .distinct()
                .forEach(name -> checkFragmentLoop(fragmentDefinitions.get(name), fragmentNames));

        fragmentNames.remove(fragmentName);
    }
}
