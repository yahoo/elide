package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;

import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.Value;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final ElideSettings elideSettings;

    private final Map<String, List<EntityProjection>> fragmentToProjections = new HashMap<>();
    private final Map<String, FragmentDefinition> fragmentDefinitions = new HashMap<>();

    /**
     * Constructor.
     *
     * @param entityDictionary entityDictionary of current Elide instance
     * @param elideSettings setting of current Elide instance
     */
    public GraphQLEntityProjectionMaker(EntityDictionary entityDictionary, ElideSettings elideSettings) {
        this.entityDictionary = entityDictionary;
        this.elideSettings = elideSettings;
    }

    private final List<EntityProjection> rootProjections = new ArrayList<>();

    /**
     * Convert a GraphQL query string into a collection of Elide {@link EntityProjection}s.
     *
     * @param query GraphQL query
     * @return all projections in the query
     */
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

        // make sure there is no fragment loop and undefined fragments in fragment definitions
        final Set<String> fragmentNames = new HashSet<>();
        fragmentDefinitions.values()
                .forEach(fragmentDefinition -> validateFragment(fragmentDefinition, fragmentNames));

        // make sure all fragments in projections are defined
        if (fragmentToProjections.keySet().stream()
                .anyMatch(fragmentName -> !fragmentDefinitions.containsKey(fragmentName))) {
            throw new InvalidEntityBodyException("Unknown fragments {" +
                    fragmentToProjections.keySet().stream()
                            .filter(fragmentName -> !fragmentDefinitions.containsKey(fragmentName))
                            .collect(Collectors.joining(",")) +
                    "}.");
        }

        // as there is no loop in the fragment definition, this loop would eventually end
        while(!fragmentToProjections.isEmpty()) {
            fragmentDefinitions.forEach((fragmentName, fragmentDefinition) -> {
                if (fragmentToProjections.containsKey(fragmentName)) {
                    List<Selection> fragmentFields = fragmentDefinition.getSelectionSet().getSelections();
                    List<EntityProjection> projections = fragmentToProjections.remove(fragmentName);

                    // the entity type of the projection must be the same type in the fragment type condition
                    String fragmentTypeName = fragmentDefinition.getTypeCondition().getName();

                    // add fields into projections, if new fragments are referenced, they would be added into the map
                    projections.stream()
                            .filter(projection -> projection.getName().equals(fragmentTypeName))
                            .forEach(projection -> fragmentFields.forEach(field -> addField(field, projection)));
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

        rootProjections.add(createProjection((Field) entitySelection));
    }

    /**
     * Construct an {@link EntityProjection} from a GraphQL {@link Field} which has arguments and selection set.
     *
     * @return constructed {@link EntityProjection}
     */
    private EntityProjection createProjection(Field entityField) {
        String entityName = entityField.getName();
        Class<?> entityType = entityDictionary.getEntityClass(entityName);
        if (entityType == null) {
            throw new InvalidEntityBodyException(String.format("Unknown entity {%s}.", entityName));
        }

        final EntityProjection entityProjection = EntityProjection.builder()
                .dictionary(entityDictionary)
                .type(entityType)
                .build();

        entityField.getArguments().forEach(argument -> addArgument(argument, entityProjection));

        SelectionSet edges = entityField.getSelectionSet();
        SelectionSet node = addEdges(edges, entityProjection);
        SelectionSet fields = addNode(node, entityProjection);
        fields.getSelections().forEach(fieldSelection -> addField(fieldSelection, entityProjection));

        return entityProjection;
    }

    /**
     * Get information of an {@link EntityProjection} from Elide 'edges' container and get the Elide 'node' container
     *
     * @param edges Elide 'edges' container
     * @param entityProjection projection that links with this container
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
     * Get information of an {@link EntityProjection} from Elide 'node' container and get fields from the container
     *
     * @param node Elide 'node' container
     * @param entityProjection projection that links with this container
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
                EntityProjection relationshipProjection = createProjection(field);

                // add this relationship projection to its parent projection
                parentProjection.getRelationships().put(fieldName, relationshipProjection);

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
            } else {
                throw new InvalidEntityBodyException(
                        String.format("Unknown attribute field {%s.%s}.", parentProjection.getName(), fieldName));
            }
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unsupported selection type {%s}.", fieldSelection.getClass()));
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
        Value argumentValue = argument.getValue();

        if (isPaginationArgument(argumentName)) {
            addPagination(argumentName, argumentValue, entityProjection);
        } else if (isSortingArgument(argumentName)) {
            addSorting(argumentValue, entityProjection);
        } else {
            Class<?> entityType = entityProjection.getType();

            if (!entityDictionary.isValidField(entityType, argumentName)) {
                // invalid argument name
                throw new IllegalStateException(
                        String.format("Unknown argument field {%s.%s}.", entityType, argument));
            }

            Attribute argumentAttribute = entityProjection.getAttributeByName(argumentName);
            com.yahoo.elide.request.Argument elideArgument = com.yahoo.elide.request.Argument.builder()
                    .name(argumentName)
                    .value(argumentValue)
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
     * @param paginationArgument  A string that contains the key to a value of sorting spec
     * @param paginationValue  A string that contains the value of pagination spec
     * @param entityProjection projection that has the pagination argument
     */
    private void addPagination(
            String paginationArgument,
            Object paginationValue,
            EntityProjection entityProjection
    ) {
        Pagination pagination = entityProjection.getPagination() == null
                ? Pagination.getDefaultPagination(elideSettings)
                : entityProjection.getPagination();

        int value = Integer.parseInt((String) paginationValue);
        if (ModelBuilder.ARGUMENT_FIRST.equals(paginationArgument)) {
            pagination.setFirst(value);
        } else if (ModelBuilder.ARGUMENT_AFTER.equals(paginationArgument)) {
            pagination.setOffset(value);
        } else {
            throw new InvalidEntityBodyException(
                    String.format("Unrecognized pagination argument '%s'", paginationArgument));
        }

        entityProjection.setPagination(pagination);
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
     * @param argumentValue A string that contains the value of sorting spec
     */
    private void addSorting(Object argumentValue, EntityProjection entityProjection) {
        String sortRule = (String) argumentValue;
        Sorting sorting = Sorting.parseSortRule(sortRule.substring(1, sortRule.length() - 1));

        entityProjection.setSorting(sorting);
    }

    /**
     * Recursive DFS to validate that there is not reference loop in a fragment and there is not un-defined
     * fragments.
     *
     * @param fragmentDefinition fragment to be checked
     * @param fragmentNames fragment names appear in the current check path
     */
    private void validateFragment(FragmentDefinition fragmentDefinition, Set<String> fragmentNames) {
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
                .forEach(name -> {
                    if (!fragmentDefinitions.containsKey(name)) {
                        throw new InvalidEntityBodyException(String.format("Unknown fragment {%s}.", name));
                    }
                    validateFragment(fragmentDefinitions.get(name), fragmentNames);
                });

        fragmentNames.remove(fragmentName);
    }
}
