/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import lombok.Getter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The id for any given entity might be populated at transaction commit (as opposed to inline with the data fetch).
 * This class wraps a {@link PersistentResource} object and allows deferred deserialization of the ID field until
 * when it is populated and when the GraphQL response is generated.
 */
@JsonSerialize(using = SerializeId.class)
public class DeferredId {
    @Getter private PersistentResource resource;

    public DeferredId(PersistentResource resource) {
        this.resource = resource;
    }
}

/**
 * Serializer for the id value of a {@link DeferredId} object.
 */
class SerializeId extends ValueSerializer<DeferredId> {
    @Override
    public void serialize(DeferredId deferredId, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) {
        jsonGenerator.writePOJO(deferredId.getResource().getId());
    }
}
