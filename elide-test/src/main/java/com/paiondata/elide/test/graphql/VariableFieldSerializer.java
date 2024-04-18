/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * A Jackson serializer for String entity field.
 * <p>
 * {@link VariableFieldSerializer} serializes a String field differently when value can both represents a concrete value
 * or a GraphQL variable. On concrete value, it outputs the original value unmodified and quoted; on variable value, it
 * does not quote the original value.
 * <p>
 * For example, given the following entity:
 * <pre>
 * {@code
 * public class Book {
 *
 *     {@literal @}JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
 *     private String title;
 * }
 * }
 * </pre>
 * A {@code Book(title="Java Concurrency in Practice")} serializes to
 * <pre>
 * {"title": "Java Concurrency in Practice"}
 * </pre>
 * However a {@code Book(title="$titlePassedByClient")} serializes to
 * <pre>
 * {"title": $titlePassedByClient}
 * </pre>
 * Note in the 1st serialization {@code title} value is quoted while the 2nd serialization it is not.
 * <p>
 * To serialize a String entity field in such a way, add the following annotation to the field, as shown above:
 * <pre>
 * {@code
 * {@literal @}JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
 * }
 * </pre>
 *
 * @see <a href="https://graphql.org/learn/queries/#variables">Variables</a>
 */
public class VariableFieldSerializer extends JsonSerializer<String> {

    private static final String VARIABLE_SIGN = "$";
    private static final String ENUM_SIGN = "#";

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        if (value.startsWith(VARIABLE_SIGN)) {
            // this is a variable
            gen.writeRawValue(value);
        } else if (value.startsWith(ENUM_SIGN)) {
            // this is an enum
            gen.writeRawValue(value.substring(1));
        } else {
            gen.writeString(value);
        }
    }
}
