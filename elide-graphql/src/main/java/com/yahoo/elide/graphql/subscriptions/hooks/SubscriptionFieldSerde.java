/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions.hooks;

import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Custom GSON serializer and deserializer that uses an Elide serde to convert a subscription field
 * to and from a primitive type.  The Serde must support deserialization from a String.
 * @param <T> The object type being serialized or deserialized.
 */
public class SubscriptionFieldSerde<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    Serde<Object, T> elideSerde;

    public SubscriptionFieldSerde(Serde<Object, T> elideSerde) {
        this.elideSerde = elideSerde;
    }

    @Override
    public JsonElement serialize(T t, Type type, JsonSerializationContext jsonSerializationContext) {
        if (t == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(String.valueOf(elideSerde.serialize(t)));
    }

    @Override
    public T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        if (jsonElement.isJsonNull()) {
            return null;
        }

        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isString()) {
                return elideSerde.deserialize(primitive.getAsString());
            }
        }

        throw new UnsupportedOperationException("Cannot deserialization subscription field: "
                + jsonElement.getAsString());
    }
}
