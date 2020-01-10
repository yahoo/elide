/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.scalars;

import com.yahoo.elide.graphql.ElideCoercing;
import com.yahoo.elide.graphql.ElideScalarType;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.utils.coerce.converters.Serde;
import graphql.language.StringValue;
import graphql.schema.CoercingParseValueException;

import java.time.OffsetDateTime;

@ElideScalarType(type = OffsetDateTime.class, name = "OffsetDateTime",
        description = "Scalar to handle java.time.OffsetDateTime Type",
        usesSerdeOfType = OffsetDateTime.class)
public class OffsetDateTimeScalar implements ElideCoercing<OffsetDateTime, Object> {
    private static final String ERROR_BAD_EPOCH_TYPE = "OffsetDateTime must be provided as string";

    @Override
    public Object serialize(Object o) {
        Serde<Object, OffsetDateTime> dateSerde = CoerceUtil.lookup(OffsetDateTime.class);
        return dateSerde.serialize((OffsetDateTime) o);
    }

    @Override
    public OffsetDateTime parseValue(Object o) {
        Serde<Object, OffsetDateTime> dateSerde = CoerceUtil.lookup(OffsetDateTime.class);
        return dateSerde.deserialize(o);
    }

    @Override
    public OffsetDateTime parseLiteral(Object o) {
        Object input;
        if (o instanceof StringValue) {
            input = ((StringValue) o).getValue();
        } else {
            throw new CoercingParseValueException(ERROR_BAD_EPOCH_TYPE);
        }
        return parseValue(input);
    }

    @Override
    public Serde getSerde() {
        return new OffsetDateTimeSerde();
    }
}
