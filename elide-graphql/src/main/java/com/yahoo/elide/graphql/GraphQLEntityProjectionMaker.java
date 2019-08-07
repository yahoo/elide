/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;

import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import graphql.GraphQL;
import graphql.parser.antlr.GraphqlBaseVisitor;
import graphql.parser.antlr.GraphqlLexer;
import graphql.parser.antlr.GraphqlParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
 * TODO - transform sorting
 * TODO - transform pagination
 * TODO - support directive since it's required by specification
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
     * A map from EntityProjection.type to EntityProjection.
     */
    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<?>, EntityProjection> projectionsByType = new HashMap<>();

    /**
     * A stack helper structure used while performing depth-first walk of GraphQL parse tree.
     * <p>
     * When we see an entity node("field" in GraphQL grammar), we push the type of this entity to this stack so that
     * we could easily pop out the entity type when we visit child node of this entity(or "field") node. This will be
     * very useful when we need to know, for example, "what is the parent entity that includes this 'id' field?".
     */
    @Getter(AccessLevel.PRIVATE)
    private final Deque<Class<?>> parentEntityType = new LinkedList<>();

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final EntityDictionary dictionary;

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

        String entityName = ctx.name().getText();
        Class<?> entityType = getDictionary().getType(entityName);

        if (!(ctx.getParent().getParent().getParent() instanceof GraphqlParser.FieldContext)) {
            // a root field
            EntityProjection rootProjection = EntityProjection.builder()
                    .dictionary(getDictionary())
                    .type(entityType)
                    .build();

            addProjection(rootProjection);
            getProjectionsByType().put(entityType, rootProjection);

            return super.visitField(ctx);
        }

        // not a root field, then there must be a prent
        String parentEntity = ((GraphqlParser.FieldContext) ctx.getParent().getParent().getParent()).name().getText();
        Class<?> parentType = getDictionary().getType(parentEntity);

        if (entityType != null) {
            // a relationship
            EntityProjection relationshipProjection = EntityProjection.builder()
                    .dictionary(getDictionary())
                    .type(entityType)
                    .build();

            getProjectionsByType().get(parentType).getRelationships().put(entityName, relationshipProjection);
            getProjectionsByType().put(entityType, relationshipProjection);

            return super.visitField(ctx);
        }

        // not a root field; not a relationship; this must be an Attribute
        Class<?> fieldType = getDictionary().getType(parentType, entityName);

        if (fieldType != null) {
            Set<Attribute> existingAttributes = new HashSet<>(getProjectionsByType().get(parentType).getAttributes());
            Attribute matchedAttribute = getProjectionsByType().get(parentType).getAttributeByName(entityName);

            if (matchedAttribute == null) {
                // this is a new attribute, add it
                existingAttributes.add(
                        Attribute.builder()
                                .type(fieldType)
                                .name(entityName)
                                .build()
                );

                // update mutated attributes
                getProjectionsByType()
                        .get(parentType)
                        .setAttributes(existingAttributes);
            }

            return super.visitField(ctx);
        }

        // not a root field; not a relationship; not an Attribute; this must be a bad field
        String message = String.format("Unknown property '%s'", entityName);
        log.error(entityName);
        throw new IllegalStateException(message);
    }

    @Override
    public Void visitArgument(final GraphqlParser.ArgumentContext ctx) {
        // GraphQL grammar for argument (6.0):
        //     argument : name ':' valueWithVariable;
        String argumentName = ctx.name().getText();
        Object argumentValue = visitValueWithVariable(ctx.valueWithVariable()); //

        // argument must comes with parent
        String entityName = ((GraphqlParser.FieldContext) ctx.getParent().getParent()).name().getText();
        Class<?> entityType = getDictionary().getType(entityName);

        if (!getDictionary().isValidField(entityType, argumentName)) {
            // invalid argument name
            String message = String.format("'%s' is not a field in '%s'", argumentName, entityName);
            log.error(message);
            throw new IllegalStateException(message);
        }

        Attribute targetAttribute = getProjectionsByType().get(entityType).getAttributeByName(argumentName);
        Argument newArgument = Argument.builder()
                .name(argumentName)
                .value(argumentValue)
                .build();

        if (targetAttribute == null) {
            Class<?> attributeType = getDictionary().getType(entityType, argumentName);
            targetAttribute = Attribute.builder()
                    .type(attributeType)
                    .name(argumentName)
                    .argument(newArgument)
                    .build();

            Set<Attribute> existingAttribute = new HashSet<>(getProjectionsByType().get(entityType).getAttributes());
            existingAttribute.add(targetAttribute);
            getProjectionsByType().get(entityType).setAttributes(existingAttribute);
        } else {
            targetAttribute.getArguments().add(newArgument);
        }

        return super.visitArgument(ctx);
    }

    @Override
    public Void visitValueWithVariable(final GraphqlParser.ValueWithVariableContext ctx) {
        return super.visitValueWithVariable(ctx);
    }

    private <T> T withParentEntity(
            Class<?> parentEntity,
            Supplier<T> actionUnderEntity
    ) {
        getParentEntityType().push(parentEntity);

        actionUnderEntity.get();

        getParentEntityType().pop();

        return null;
    }

    private void addProjection(EntityProjection newProjection) {
        getAllEntityProjections().add(newProjection);
        getProjectionsByType().put(newProjection.getType(), newProjection);
    }
}
