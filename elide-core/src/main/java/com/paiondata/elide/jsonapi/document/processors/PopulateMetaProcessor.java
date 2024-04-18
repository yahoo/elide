/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.jsonapi.document.processors;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Meta;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            Map<String, List<String>> queryParams
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
            Map<String, List<String>> queryParams
    ) {
        addDocumentMeta(jsonApiDocument, scope);
    }
}
