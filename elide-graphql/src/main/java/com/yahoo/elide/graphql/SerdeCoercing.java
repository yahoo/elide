/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.utils.coerce.converters.Serde;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SerdeCoercing<I, O> implements Coercing<I, O> {
    private String errorMessage;
    private Serde<O, I> serde;

    @Override
    public O serialize(Object dataFetcherResult) {
        return serde.serialize((I) dataFetcherResult);
    }

    @Override
    public I parseValue(Object input) {
        return serde.deserialize((O) input);
    }

    public I parseLiteral(Object o) {
        Object input;
        if (o instanceof IntValue) {
            input = ((IntValue) o).getValue().longValue();
        } else if (o instanceof StringValue) {
            input = ((StringValue) o).getValue();
        } else if (o instanceof FloatValue) {
            input = ((FloatValue) o).getValue().floatValue();
        } else {
            throw new CoercingParseValueException(errorMessage);
        }
        return parseValue(input);
    }
}
