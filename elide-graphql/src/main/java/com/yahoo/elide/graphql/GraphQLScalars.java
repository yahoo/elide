/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Date;

/**
 * Additional scalar/serializers for built-in graphql types.
 */
@Slf4j
public class GraphQLScalars {
    private static final String ERROR_BAD_EPOCH = "Date must be provided as epoch millis";
    private static final String ERROR_BAD_EPOCH_TYPE = "Date must be provided as string or integral in epoch millis";

    public static final GraphQLScalarType GRAPHQL_DATE_TYPE = new GraphQLScalarType(
            "Date",
            "Built-in date",
            new Coercing<Date, Long>() {
                @Override
                public Long serialize(Object o) {
                    return o == null ? 0L : ((Date) o).getTime();
                }

                @Override
                public Date parseValue(Object o) {
                    // Expects epoch millis:
                    Long timeValue;
                    if (o instanceof String) {
                        try {
                            timeValue = Long.parseLong((String) o);
                        } catch (NumberFormatException e) {
                            log.debug("Failed to convert string to epoch time for date conversion", e);
                            throw new CoercingParseValueException(ERROR_BAD_EPOCH);
                        }
                    } else if (o instanceof Number) {
                        timeValue = ((Number) o).longValue();
                    } else {
                        log.debug("Input value {} was not valid epoch millis type", o);
                        throw new CoercingParseValueException(ERROR_BAD_EPOCH);
                    }
                    return Date.from(Instant.ofEpochMilli(timeValue));
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
            }
    );
}
