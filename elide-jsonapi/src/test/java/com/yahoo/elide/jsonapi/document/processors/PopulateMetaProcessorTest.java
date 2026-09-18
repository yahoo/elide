/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi.document.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Resource;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PopulateMetaProcessorTest {

    @Test
    public void testRequestScopeWithMeta() {
        RequestScope scope = mock(RequestScope.class);
        when(scope.getMetadataFields()).thenReturn(Set.of("foo"));
        when(scope.getMetadataField(eq("foo"))).thenReturn(Optional.of("bar"));

        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getRequestScope()).thenReturn(scope);

        PopulateMetaProcessor metaProcessor = new PopulateMetaProcessor();
        JsonApiDocument doc = new JsonApiDocument();
        doc.setData(new Data<Resource>(List.of()));
        metaProcessor.execute(doc, scope, new LinkedHashSet<>(Set.of(persistentResource)), null);

        Meta meta = doc.getMeta();
        assertNotNull(meta);
        assertEquals(1, meta.getMetaMap().size());
        assertEquals("bar", meta.getMetaMap().get("foo"));
    }
}
