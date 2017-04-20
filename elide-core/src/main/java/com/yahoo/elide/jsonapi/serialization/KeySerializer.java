/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Date;

/**
 * Custom serializer for Serializing Map Keys.
 * Change from StdKeySerializer - In cases of enum value it uses
 * name() instead of defaulting to toString() since that may be overridden
 */
public class KeySerializer extends StdSerializer<Object> {
    public KeySerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        String str;
        Class<?> cls = value.getClass();

        if (cls == String.class) {
            str = (String) value;
        } else if (Date.class.isAssignableFrom(cls)) {
            provider.defaultSerializeDateKey((Date) value, jgen);
            return;
        } else if (cls == Class.class) {
            str = ((Class<?>) value).getName();
        } else if (cls.isEnum()) {
            str = ((Enum<?>) value).name();
        } else {
            str = value.toString();
        }
        jgen.writeFieldName(str);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException {
        visitStringFormat(visitor, typeHint);
    }
}
