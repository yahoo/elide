/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.test.graphql;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * A Jackson serializer for String entity field.
 */
public class EnumFieldSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializationContext) {
            gen.writeRawValue(value);
    }
}
