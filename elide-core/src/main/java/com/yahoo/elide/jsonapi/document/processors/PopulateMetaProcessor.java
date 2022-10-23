/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi.document.processors;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;

import java.util.HashMap;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Document processor that populates 'meta' fields for collections (from request scope metadata) and
 * resources (from resource metadata).  This processor runs after the document has already been populated.
 */
public class PopulateMetaProcessor implements DocumentProcessor {
    @Override
    public void execute(
            JsonApiDocument jsonApiDocument,
            PersistentResource resource,
            MultivaluedMap<String, String> queryParams
    ) {
        if (resource == null) {
            return;
        }
        Meta meta = jsonApiDocument.getData().getSingleValue().getMeta();

        Object obj = resource.getObject();
        if (! (obj instanceof WithMetadata)) {
            return;
        }

        WithMetadata withMetadata = (WithMetadata) obj;
        Set<String> fields = withMetadata.getMetadataFields();

        if (fields.size() == 0) {
            return;
        }

        if (meta == null) {
            meta = new Meta(new HashMap<>());
        }
        for (String field : fields) {
            meta.getMetaMap().put(field, withMetadata.getMetadataField(field).get());
        }

        jsonApiDocument.getData().getSingleValue().setMeta(meta);
    }

    @Override
    public void execute(
            JsonApiDocument jsonApiDocument,
            Set<PersistentResource> resources,
            MultivaluedMap<String, String> queryParams) {
        if (resources.size() == 0) {
            return;
        }
        Meta meta = jsonApiDocument.getMeta();
        RequestScope scope = resources.iterator().next().getRequestScope();

        Set<String> fields = scope.getMetadataFields();
        if (fields.size() == 0) {
            return;
        }

        if (meta == null) {
            meta = new Meta(new HashMap<>());
        }

        for (String field : fields) {
            meta.getMetaMap().put(field, scope.getMetadataField(field).get());
        }

        jsonApiDocument.setMeta(meta);
    }
}
