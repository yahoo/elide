/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import com.paiondata.elide.jsonapi.models.Data;
import com.paiondata.elide.jsonapi.models.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Custom serializer for top-level data.
 */
public class DataSerializer extends JsonSerializer<Data<Resource>> {

    @Override
    public void serialize(Data<Resource> data, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {
        Collection<Resource> list = data.get();
        if (data.isToOne()) {
            if (CollectionUtils.isEmpty(list)) {
                jsonGenerator.writeObject(null);
                return;
            }
            jsonGenerator.writeObject(IterableUtils.first(list));
            return;
        }
        jsonGenerator.writeObject((list == null) ? Collections.emptyList() : list);
    }
}
