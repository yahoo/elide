/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A simplified object that models a GraphQL object in a single string.
 * <p>
 * {@link ObjectValueWithVariable} implements the {@code objectValueWithVariable} defined in GraphQL grammar.
 *
 * @see ValueWithVariable
 */
@RequiredArgsConstructor
public class ObjectValueWithVariable implements ValueWithVariable {

    private static final long serialVersionUID = 6768988285154422347L;

    /**
     * GraphQL argument name is unquoted; hence quoted field is disabled.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .configure(
                    JsonGenerator.Feature.QUOTE_FIELD_NAMES,
                    false
            )
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final Object object;

    @Override
    public String toGraphQLSpec() {
        try {
            return JSON_MAPPER.writeValueAsString(getObject());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
