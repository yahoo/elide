/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.containers.ConnectionContainer;
import com.yahoo.elide.graphql.containers.EdgesContainer;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import graphql.GraphQL;
import graphql.parser.antlr.GraphqlBaseVisitor;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import javafx.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A strategy that mapps a read-only GraphQL query to a collection of {@link EntityProjection}s.
 * <p>
 * <b>{@link GraphQLEntityProjectionMaker} produces {@link EntityProjection} that is only used in read operations</b>.
 * Any write operations will result in runtime exceptions, although they might be supported in future releases. GraphQL
 * query involving write operations should be processed by GraphQL API instead({@link GraphQL#execute(String)}).
 * <p>
 * {@link GraphQLEntityProjectionMaker} is not thread-safe and its concurrent access must be guarded by external
 * synchronizations.
 * <p>
 * Caller should not invoke any {@link GraphQLEntityProjectionMaker} methods except for its constructor and
 * {@link #make(String)}.
 *
 * @see EntityProjection
 * @see ModelBuilder
 * @see <a href="http://elide.io/pages/guide/11-graphql.html#sorting">Sorting</a>
 *
 * @// TODO: 2019-08-07 Support fragment query and update elide doc to include example of frangment query
 */
@Slf4j
@RequiredArgsConstructor
public class GraphQLEntityProjectionMaker extends GraphqlBaseVisitor<Void> {

    /**
     * All EntityProjections transformed from a GraphQL query.
     * <p>
     * GraphQL can query multiple entities in a single query, each root entity is transformed in an EntityProjections in
     * this list.
     */
    @Getter(AccessLevel.PRIVATE)
    private final List<EntityProjection> allEntityProjections = new ArrayList<>();

    /**
     * A map from node path to the entityProjection.
     */
    @Getter(AccessLevel.PRIVATE)
    private final Map<String, EntityProjection> projectionsByNodePath = new HashMap<>();

    /**
     * A stack helper structure used while performing depth-first walk of GraphQL parse tree.
     * <p>
     * When we see an entity node("field" in GraphQL grammar), we push the type of this entity to this stack so that
     * we could easily peek out the entity type when we visit child node of this entity(or "field") node. This will be
     * very useful when we need to know, for example, "what is the parent entity that includes this 'id' field?". When
     * we are done with the sub-tree of this entity node, we pop it out.
     * <p>
     * To read a parent [node path, entity type] pair, use {@link Deque#peek()} and DO NOT use {@link Deque#pop()}
     * (See {@link #walkParentEntity(Class, String, Supplier)}).
     */
    @Getter(AccessLevel.PRIVATE)
    private final Deque<Pair<String, Class<?>>> parentEntityStack = new LinkedList<>();

    /**
     * Reference
     */
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Object valueWithVariable;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final EntityDictionary dictionary;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final ElideSettings elideSettings;

    public Collection<EntityProjection> make(String query) {
        ANTLRInputStream inputStream = new ANTLRInputStream(query);
        GraphqlLexer lexer = new GraphqlLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GraphqlParser parser = new GraphqlParser(tokens);

        // GraphQL grammar on document (6.0):
        //
        //     document : definition+;
        //
        // Given that GraphQL can query multiple entities in a single query,
        // we define one root level entity projection for each document.definition
        List<GraphqlParser.DefinitionContext> definitions = parser.document().definition();

        definitions.forEach(this::visitDefinition);

        return allEntityProjections;
    }

    @Override
    public Void visitDefinition(final GraphqlParser.DefinitionContext ctx) {
        // GraphQL grammar on a single definition (6.0):
        //
        // definition:
        //     operationDefinition |
        //     fragmentDefinition |
        //     typeSystemDefinition
        //     ;

        if (ctx.typeSystemDefinition() != null) {
            String message = "Type System Definition is not allowed in read-only operations";
            log.error(message);
            throw new IllegalStateException(message);
        }

        // nothing interesting about entity projection here; go to child nodes down the parse tree
        return super.visitDefinition(ctx);
    }

    @Override
    public Void visitField(final GraphqlParser.FieldContext ctx) {
        // GraphQL grammar for field (6.0):
        //     field : alias? name arguments? directives? selectionSet?;

        String nodeName = ctx.name().getText();
        if (ConnectionContainer.EDGES_KEYWORD.equals(nodeName)
                || EdgesContainer.NODE_KEYWORD.equals(nodeName)
                || ConnectionContainer.PAGE_INFO_KEYWORD.equals(nodeName)
        ) {
            // do nothing on "edges", "node", or "pageInfo" field 
            // see https://graphql.org/learn/pagination/#pagination-and-edges
            // https://elide.io/pages/guide/11-graphql.html#relationship-metadata
            return super.visitField(ctx);
        }

        if (getParentEntityStack().isEmpty()) {
            // a root field; walk sub-tree rooted at this entity
            Class<?> entityType = getDictionary().getEntityClass(nodeName);

            return walkParentEntity(
                    entityType,
                    nodeName,
                    () -> {
                        EntityProjection rootProjection = EntityProjection.builder()
                                .dictionary(getDictionary())
                                .type(entityType)
                                .build();

                        addProjection(rootProjection, nodeName);

                        return super.visitField(ctx);
                    });
        }

        // not a root field, then there must be a parent
        final Pair<String, Class<?>> parentPathType = Objects.requireNonNull(getParentEntityStack().peek());
        final Class<?> parentType = parentPathType.getValue();
        final String parentPath = parentPathType.getKey();
        final EntityProjection parentProjection = getProjectionsByNodePath().get(parentPath);

        // new path for current node
        String nodePath = parentPath + "." + nodeName;

        // check whether this field is a relationship field
        if (getDictionary().getRelationshipType(parentType, nodeName) != RelationshipType.NONE) {
            final Class<?> relationshipType =
                    this.getDictionary().getRelationshipParameterType(parentType, nodeName) == null
                    ? this.getDictionary().getType(parentType, nodeName)
                    : this.getDictionary().getRelationshipParameterType(parentType, nodeName);

            // a relationship field; walk sub-tree rooted at this relationship entity
            return walkParentEntity(
                    relationshipType,
                    nodePath,
                    () -> {
                        EntityProjection relationshipProjection = EntityProjection.builder()
                                .dictionary(getDictionary())
                                .type(relationshipType)
                                .build();

                        parentProjection.getRelationships().put(nodeName, relationshipProjection);
                        getProjectionsByNodePath().put(nodePath, relationshipProjection);

                        return super.visitField(ctx);
                    });
        }

        // not a root field; not a relationship field; this must be an Attribute
        Class<?> attributeType = getDictionary().getType(parentType, nodeName);

        if (attributeType != null) {
            Set<Attribute> existingAttributes = new HashSet<>(parentProjection.getAttributes());
            Attribute matchedAttribute = parentProjection.getAttributeByName(nodeName);

            if (matchedAttribute == null) {
                // this is a new attribute, add it
                existingAttributes.add(
                        Attribute.builder()
                                .type(attributeType)
                                .name(nodeName)
                                .build()
                );

                // update mutated attributes
                parentProjection.setAttributes(existingAttributes);
            }

            return super.visitField(ctx);
        }

        // not a root field; not a relationship; not an Attribute; this must be a bad field
        String message = String.format("Unknown property '%s'", nodeName);
        log.error(nodeName);
        throw new IllegalStateException(message);
    }

    @Override
    public Void visitArgument(final GraphqlParser.ArgumentContext ctx) {
        // GraphQL grammar for argument (6.0):
        //     field : alias? name arguments? directives? selectionSet?;
        //     argument : name ':' valueWithVariable;

        String argumentName = ctx.name().getText();
        Object argumentValue = visitTerminalValue(ctx.valueWithVariable()); //

        if (isPaginationArgument(argumentName)) {
            attachPagination(argumentName, argumentValue);
            return null; // this is a terminal node
        }

        if (isSortingArgument(argumentName)) {
            attachSorting(argumentValue);
            return null; // this is a terminal node
        }

        // argument must comes with parent
        final Class<?> parentType = Objects.requireNonNull(getParentEntityStack().peek()).getValue();
        final String parentNodePath = Objects.requireNonNull(getParentEntityStack().peek()).getKey();
        final EntityProjection parentProjection = getProjectionsByNodePath().get(parentNodePath);

        if (!getDictionary().isValidField(parentType, argumentName)) {
            // invalid argument name
            String message = String.format("'%s' is not a field in '%s'", argumentName, parentType);
            log.error(message);
            throw new IllegalStateException(message);
        }

        Attribute targetAttribute = parentProjection.getAttributeByName(argumentName);
        Argument newArgument = Argument.builder()
                .name(argumentName)
                .value(argumentValue)
                .build();

        if (targetAttribute == null) {
            Class<?> attributeType = getDictionary().getType(parentType, argumentName);
            targetAttribute = Attribute.builder()
                    .type(attributeType)
                    .name(argumentName)
                    .argument(newArgument)
                    .build();

            Set<Attribute> existingAttribute = new HashSet<>(parentProjection.getAttributes());
            existingAttribute.add(targetAttribute);
            parentProjection.setAttributes(existingAttribute);
        } else {
            targetAttribute.getArguments().add(newArgument);
        }

        return null; // this is a terminal node
    }

    @Override
    public Void visitValueWithVariable(final GraphqlParser.ValueWithVariableContext ctx) {
        /*
         * GraphQL grammar for valueWithVariable (6.0):
         *
         *     valueWithVariable :
         *     variable |
         *     IntValue |
         *     FloatValue |
         *     StringValue |
         *     BooleanValue |
         *     NullValue |
         *     enumValue |
         *     arrayValueWithVariable |
         *     objectValueWithVariable;
         */
        if (ctx.IntValue() != null) {
            setValueWithVariable(ctx.IntValue().getText());
        } else if (ctx.FloatValue() != null) {
            setValueWithVariable(ctx.FloatValue().getText());
        } else if (ctx.StringValue() != null) {
            setValueWithVariable(ctx.StringValue().getText());
        } else if (ctx.BooleanValue() != null) {
            setValueWithVariable(ctx.BooleanValue().getText());
        } else if (ctx.NullValue() != null) {
            setValueWithVariable(ctx.NullValue().getText());
        } else {
            // TODO - support enumValue, variable, arrayValueWithVariable, and objectValueWithVariable ASAP
            String message = "Non-primitive-typed argument will be supported in future release";
            log.error(message);
            throw new IllegalStateException(message);
        }

        return null;
    }

    /**
     * Returns whether or not a GraphQL argument name corresponding to a pagination argument.
     *
     * @param argumentName  Name key of the GraphQL argument
     *
     * @return {@code true} if the name equals to {@link ModelBuilder#ARGUMENT_FIRST} or
     * {@link ModelBuilder#ARGUMENT_AFTER}
     */
    private static boolean isPaginationArgument(String argumentName) {
        return ModelBuilder.ARGUMENT_FIRST.equals(argumentName) || ModelBuilder.ARGUMENT_AFTER.equals(argumentName);
    }

    /**
     * Creates a {@link Pagination} object from pagination GraphQL argument and attaches it to the entity paginated
     * according to the newly created {@link Pagination} object.
     *
     * @param paginationArgument  A string that contains the key to a value of sorting spec
     * @param paginationValue  A string that contains the value of pagination spec
     */
    private void attachPagination(String paginationArgument, Object paginationValue) {
        // there must be an entity to apply this pagination
        String paginatedEntityNodePath = Objects.requireNonNull(getParentEntityStack().peek()).getKey();

        EntityProjection entityProjection = getProjectionsByNodePath().get(paginatedEntityNodePath);

        Pagination pagination = entityProjection.getPagination() == null
                ? Pagination.getDefaultPagination(getElideSettings())
                : entityProjection.getPagination();

        Integer value = Integer.parseInt((String) paginationValue);
        if (ModelBuilder.ARGUMENT_FIRST.equals(paginationArgument)) {
            pagination.setFirst(value);
        } else if (ModelBuilder.ARGUMENT_AFTER.equals(paginationArgument)) {
            pagination.setOffset(value);
        } else {
            String message = String.format("Unrecognized pagination argument '%s'", paginationArgument);
        }

        entityProjection.setPagination(pagination);
    }

    /**
     * Returns whether or not a GraphQL argument name corresponding to a sorting argument.
     *
     * @param argumentName  Name key of the GraphQL argument
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
     * @param argumentValue  A string that contains the value of sorting spec
     */
    private void attachSorting(Object argumentValue) {
        // there must be an entity to apply this sorting
        String sortedEntityNodePath = Objects.requireNonNull(getParentEntityStack().peek()).getKey();

        EntityProjection entityProjection = getProjectionsByNodePath().get(sortedEntityNodePath);

        String sortRule = (String) argumentValue;
        Sorting sorting = Sorting.parseSortRule(sortRule.substring(1, sortRule.length() - 1));

        entityProjection.setSorting(sorting);
    }

    /**
     * Puts an entity type onto top of stack, which is {@link #parentEntityStack}, performs an depth-first walk rooted
     * at this entity (including this root entity), and pops out the entity type.
     *
     * @param entityClass The entity type to be pushed the stack
     * @param nodePath Node path to this entity
     * @param actionUnderEntity  The depth-first walk after the push
     *
     * @param <T>  The type of return value as a result of the depth-first walk.
     *
     * @return {@code null}
     */
    private <T> T walkParentEntity(
            Class<?> entityClass,
            String nodePath,
            Supplier<T> actionUnderEntity
    ) {
        getParentEntityStack().push(new Pair<>(nodePath, entityClass));

        actionUnderEntity.get(); // perform action

        getParentEntityStack().pop();

        return null;
    }

    /**
     * Visit terminal node in parse tree and sets the last terminal value.
     *
     * @param ctx  The parent node of the visited terminal node
     *
     * @return the value on the terminal node
     */
    private Object visitTerminalValue(GraphqlParser.ValueWithVariableContext ctx) {
        visitValueWithVariable(ctx);
        return getValueWithVariable();
    }

    /**
     * Records one newly constructed {@link EntityProjection} to the final result list and cache it by root entity type
     * for quick reference later.
     *
     * @param newProjection  The new {@link EntityProjection} to be added
     */
    private void addProjection(EntityProjection newProjection, String nodePath) {
        getAllEntityProjections().add(newProjection);
        getProjectionsByNodePath().put(nodePath, newProjection);
    }
}
