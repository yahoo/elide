/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ResourceLineageTest {

    @Test
    public void testGetParent() {
        ResourceLineage lineage = new ResourceLineage();
        assertNull(lineage.getParent());

        PersistentResource authorResource = mock(PersistentResource.class);
        PersistentResource bookResource = mock(PersistentResource.class);

        ResourceLineage longerLineage = new ResourceLineage(lineage, authorResource, "authors");
        assertEquals(authorResource, longerLineage.getParent());

        ResourceLineage longerLongerLineage = new ResourceLineage(longerLineage, bookResource, "books");
        assertEquals(bookResource, longerLongerLineage.getParent());
    }
}
