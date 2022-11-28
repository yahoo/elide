/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.Serde;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * Additional scalar/serializers for built-in graphql types.
 */
@Slf4j
public class GraphQLScalars {
    private static final String ERROR_BAD_EPOCH_TYPE = "Date must be provided as string or integral in epoch millis";

    // TODO: Should we make this a class that can be configured? Should determine if there are other customizeable
    // TODO: scalar types.
    // NOTE: Non-final so it's overrideable if someone wants _different_ date representations.
    public static GraphQLScalarType GRAPHQL_DATE_TYPE = GraphQLScalarType.newScalar()
            .name("Date")
            .description("Built-in date")
            .coercing(new Coercing<Date, Object>() {
                @Override
                public Object serialize(Object o) {
                    Serde<Object, Date> dateSerde = CoerceUtil.lookup(Date.class);

                    return dateSerde.serialize((Date) o);
                }

                @Override
                public Date parseValue(Object o) {
                    Serde<Object, Date> dateSerde = CoerceUtil.lookup(Date.class);

                    return dateSerde.deserialize(o);
                }

                @Override
                public Date parseLiteral(Object o) {
                    Object input;
                    if (o instanceof IntValue) {
                        input = ((IntValue) o).getValue().longValue();
                    } else if (o instanceof StringValue) {
                        input = ((StringValue) o).getValue();
                    } else {
                        throw new CoercingParseValueException(ERROR_BAD_EPOCH_TYPE);
                    }
                    return parseValue(input);
                }
            })
            .build();

    public static GraphQLScalarType GRAPHQL_DEFERRED_ID = GraphQLScalarType.newScalar()
            .name("DeferredID")
            .description("custom id type")
            .coercing(new Coercing() {
                @Override
                public Object serialize(Object o) {
                    return o;
                }

                @Override
                public String parseValue(Object o) {
                    return o.toString();
                }

                @Override
                public String parseLiteral(Object o) {
                    if (o instanceof StringValue) {
                        return ((StringValue) o).getValue();
                    }
                    if (o instanceof IntValue) {
                        return ((IntValue) o).getValue().toString();
                    }
                    // Unexpected object, try to use the toString.
                    log.debug("Found unexpected object type: {}", o.getClass());
                    return o.toString();
                }
            })
            .build();
}
