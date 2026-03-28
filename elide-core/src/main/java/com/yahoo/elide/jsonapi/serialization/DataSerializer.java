/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom serializer for top-level data.
 */
public class DataSerializer extends ValueSerializer<Data<Resource>> {

    @Override
    public void serialize(Data<Resource> data, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        Collection<Resource> list = data.get();
        if (data.isToOne()) {
            if (CollectionUtils.isEmpty(list)) {
                jsonGenerator.writePOJO(null);
                return;
            }
            jsonGenerator.writePOJO(IterableUtils.first(list));
            return;
        }
        jsonGenerator.writePOJO((list == null) ? Collections.emptyList() : list);
    }
}
