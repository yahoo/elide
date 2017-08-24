/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yahoo.elide.core.PersistentResource;
import lombok.Getter;

import java.io.IOException;

@JsonSerialize(using = SerializeId.class)
public class DeferredId {
    @Getter private PersistentResource resource;

    public DeferredId(PersistentResource resource) {
        this.resource = resource;
    }
}

class SerializeId extends JsonSerializer<DeferredId> {
    @Override
    public void serialize(DeferredId deferredId, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(deferredId.getResource().getId());
    }
}
