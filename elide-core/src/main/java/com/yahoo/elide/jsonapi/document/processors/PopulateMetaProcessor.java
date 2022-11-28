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
import java.util.LinkedHashSet;
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
            RequestScope scope,
            PersistentResource persistentResource,
            MultivaluedMap<String, String> queryParams
    ) {

        addDocumentMeta(jsonApiDocument, scope);
    }

    private void addDocumentMeta(JsonApiDocument document, RequestScope scope) {
        Set<String> fields = scope.getMetadataFields();
        if (fields.size() == 0) {
            return;
        }
        Meta meta = document.getMeta();
        if (meta == null) {
            meta = new Meta(new HashMap<>());
        }

        for (String field : fields) {
            meta.getMetaMap().put(field, scope.getMetadataField(field).get());
        }

        document.setMeta(meta);
    }

    @Override
    public void execute(
            JsonApiDocument jsonApiDocument,
            RequestScope scope,
            LinkedHashSet<PersistentResource> resources,
            MultivaluedMap<String, String> queryParams
    ) {
        addDocumentMeta(jsonApiDocument, scope);
    }
}
