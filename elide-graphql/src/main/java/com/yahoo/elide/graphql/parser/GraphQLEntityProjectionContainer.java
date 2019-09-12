package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import graphql.language.SourceLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
public class GraphQLEntityProjectionContainer {
    @Getter
    private final Collection<EntityProjection> projections;

    @Getter
    private final Map<SourceLocation, Relationship> relationshipMap;
}
