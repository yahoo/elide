/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
 * Caller should not call any methods on {@link GraphQLEntityProjectionMaker} except for its constructor and
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

    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<?>, EntityProjection> allEntityProjections = new HashMap<>();

    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<?>, EntityProjection> projectionsByKey = new HashMap<>();

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
        // we define one document.definition per entity projection
        List<GraphqlParser.DefinitionContext> definitions = parser.document().definition();

        definitions.forEach(this::visitDefinition);

        return allEntityProjections.values();
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
            getProjectionsByKey().put(entityType, rootProjection);

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

            getAllEntityProjections().get(parentType).getRelationships().put(entityName, relationshipProjection);
            getProjectionsByKey().put(entityType, relationshipProjection);

            return super.visitField(ctx);
        }

        // not a root field; not a relationship; this must be an Attribute
        Class<?> fieldType = getDictionary().getType(parentType, entityName);

        if (fieldType != null) {
            Set<Attribute> existingAttributes = new HashSet<>(getProjectionsByKey().get(parentType).getAttributes());
            existingAttributes.add(
                    Attribute.builder()
                            .type(fieldType)
                            .name(entityName)
                            .build()
            );

            // an attribute
            getProjectionsByKey()
                    .get(parentType)
                    .setAttributes(existingAttributes);

            return super.visitField(ctx);
        }

        // not a root field; not a relationship; not an Attribute; this must be a bad field
        String message = String.format("Unknown property '%s'", entityName);
        log.error(entityName);
        throw new IllegalStateException(message);
    }

    @Override
    public Void visitArgument(final GraphqlParser.ArgumentContext ctx) {
        // GraphQL grammar for field (6.0):
        //     argument : name ':' valueWithVariable;
        String name = ctx.name().getText();
        Object value = ctx.valueWithVariable();

        // argument must comes with parent
        String entityName = ((GraphqlParser.FieldContext) ctx.getParent().getParent()).name().getText();
        Class<?> entityType = getDictionary().getType(entityName);

        if (!getDictionary().isValidField(entityType, name)) {
            // invalid argument name
            String message = String.format("'%s' is not a '%s' field", name, entityName);
            log.error(message);
            throw  new IllegalStateException(message);
        }

        Attribute targetAttribute = getProjectionsByKey().get(entityType).getAttributeByName(name);
        Argument setArgument = Argument.builder()
                .name(name)
                .value(value)
                .build();

        if (targetAttribute == null) {
            Class<?> attributeType = getDictionary().getType(entityType, name);
            targetAttribute = Attribute.builder()
                    .type(attributeType)
                    .name(name)
                    .argument(setArgument)
                    .build();

            Set<Attribute> existingAttribute = new HashSet<>(getProjectionsByKey().get(entityType).getAttributes());
            existingAttribute.add(targetAttribute);
            getProjectionsByKey().get(entityType).setAttributes(existingAttribute);
        } else {
            targetAttribute.getArguments().add(setArgument);
        }

        return super.visitArgument(ctx);
    }

    private void addProjection(EntityProjection newProjection) {
        getAllEntityProjections().compute(newProjection.getType(), (k, existingProjection) -> {
            if (existingProjection == null) {
                return newProjection;
            } else {
                return existingProjection.merge(newProjection);
            }
        });
    }
}
