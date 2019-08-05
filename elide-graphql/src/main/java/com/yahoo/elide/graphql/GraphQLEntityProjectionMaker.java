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
 * <b>{@link GraphQLEntityProjectionMaker} produces {@link EntityProjection} that is only used in read operations</b>.
 * Read-Write GraphQL query should be processed by GraphQL API instead({@link GraphQL#execute(String)}).
 *
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

//    @Override
//    public Void visitOperationDefinition(final GraphqlParser.OperationDefinitionContext ctx) {
//        // GraphQL grammar for operation (6.0):
//        //     selectionSet |
//        //     operationType  name? variableDefinitions? directives? selectionSet;
//
//        if (ctx.operationType().MUTATION() != null) {
//            String message = "Mutation is not allowed, because entity projection is for read-only operations";
//            log.error(message);
//            throw new IllegalStateException(message);
//        }
//
//        // nothing interesting about entity projection here except for selectionSet;
//        // go to selectionSet child node down the parse tree
//        return super.visitSelectionSet(ctx.selectionSet());
//    }

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

        Attribute targetAttribute = getAllEntityProjections().get(entityType).getAttributes().stream()
                .filter(attribute -> attribute.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> {
                    String message = String.format("'%s' is not a '%s' field", name, entityName);
                    log.error(message);
                    return new IllegalStateException(message);
                });

        targetAttribute.getArguments().add(
                Argument.builder()
                        .name(name)
                        .value(value)
                        .build()
        );

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
