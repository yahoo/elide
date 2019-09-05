package com.yahoo.elide.graphql.parser;

import static com.yahoo.elide.graphql.containers.KeyWord.EDGES_KEYWORD;
import static com.yahoo.elide.graphql.containers.KeyWord.NODE_KEYWORD;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.ModelBuilder;
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
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    private final Map<String, FragmentDefinition> fragmentMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param elideSettings setting of current Elide instance
     */
    public GraphQLEntityProjectionMaker(ElideSettings elideSettings) {
        this.entityDictionary = elideSettings.getDictionary();
    }

    private final List<EntityProjection> rootProjections = new ArrayList<>();

    /**
     * Convert a GraphQL query string into a collection of Elide {@link EntityProjection}s.
     *
     * @param query GraphQL query
     * @return all projections in the query
     */
    public Collection<EntityProjection> make(String query) {
        fragmentMap.clear();

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

        return rootProjections;
    }

    /**
     * Root projection would be an operation applied on an entity class. There should be only one selection, which
     * is the entity, in the selection set. The EntityProjection tree would be constructed recursively to add all
     * child projections.
     */
    private void addRootProjection(SelectionSet selectionSet) {
        List<Selection> selections = selectionSet.getSelections();

        if (selections.size() != 1) {
            throw new InvalidEntityBodyException("Can't select multiple entities in graphQL QUERY operation.");
        }

        Selection entitySelection = selections.get(0);
        if (!(entitySelection instanceof Field)) {
            throw new InvalidEntityBodyException("Entity selection must be a graphQL field.");
        }

        String entityName = ((Field) entitySelection).getName();
        Class<?> entityType = entityDictionary.getEntityClass(entityName);
        if (entityType == null) {
            throw new InvalidEntityBodyException(String.format("Unknown entity {%s}.", entityName));
        }

        rootProjections.add(createProjection(entityType, (Field) entitySelection));
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

        entityField.getArguments().forEach(argument -> addArgument(argument, entityProjection));

        SelectionSet edges = entityField.getSelectionSet();
        SelectionSet node = resolveEdges(edges, entityProjection);
        SelectionSet fields = resolveNode(node, entityProjection);
        fields.getSelections().forEach(fieldSelection -> addSelection(fieldSelection, entityProjection));

        return entityProjection;
    }

    /**
     * Get information of an {@link EntityProjection} from Elide 'edges' container and get the Elide 'node' container
     *
     * @param edges Elide 'edges' container
     * @param entityProjection projection that links with this container
     * @return Elide 'node' container
     */
    private static SelectionSet resolveEdges(SelectionSet edges, EntityProjection entityProjection) {
        if (edges.getSelections().size() != 1) {
            throw new InvalidEntityBodyException("Entity selection must have one 'edges' graphQL field node.");
        }

        Selection edgesSelection = edges.getSelections().get(0);
        if (!(edgesSelection instanceof Field)) {
            throw new InvalidEntityBodyException("Edges selection must be a graphQL field.");
        }

        if (!EDGES_KEYWORD.equals(((Field) edgesSelection).getName())) {
            throw new InvalidEntityBodyException("GraphQL field selection must have 'edges'.");
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
    private static SelectionSet resolveNode(SelectionSet node, EntityProjection entityProjection) {
        if (node.getSelections().size() != 1) {
            throw new InvalidEntityBodyException("Edges selection must have one 'node' graphQL field node.");
        }

        Selection nodeSelection = node.getSelections().get(0);
        if (!(nodeSelection instanceof Field)) {
            throw new InvalidEntityBodyException("Node selection must be a graphQL field.");
        }

        if (!NODE_KEYWORD.equals(((Field) nodeSelection).getName())) {
            throw new InvalidEntityBodyException("GraphQL 'edges' field selection must have 'node'.");
        }

        return ((Field) nodeSelection).getSelectionSet();
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
            addField((Field) fieldSelection, parentProjection);
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
            // NOOP
        } else if (isSortingArgument(argumentName)) {
            // NOOP
        } else {
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
     * Returns whether or not a GraphQL argument name corresponding to a sorting argument.
     *
     * @param argumentName Name key of the GraphQL argument
     *
     * @return {@code true} if the name equals to {@link ModelBuilder#ARGUMENT_SORT}
     */
    private static boolean isSortingArgument(String argumentName) {
        return ModelBuilder.ARGUMENT_SORT.equals(argumentName);
    }

    private void addAttributeArgument(Argument argument, EntityProjection entityProjection) {
        String argumentName = argument.getName();
        Class<?> entityType = entityProjection.getType();

        if (!entityDictionary.isValidField(entityType, argumentName)) {
            // invalid argument name
            throw new IllegalStateException(
                    String.format("Unknown argument field {%s.%s}.", entityType, argument));
        }

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
