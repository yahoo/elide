/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.porting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.List;

class SingleResultQueryTest {

    @Test
    void scrollEmpty() {
        assertFalse(new SingleResultQuery(() -> null).scroll().iterator().hasNext());
    }

    @Test
    void listEmpty() {
        assertFalse(new SingleResultQuery(() -> null).list().iterator().hasNext());
    }

    @Test
    void uniqueResultNull() {
        assertNull(new SingleResultQuery(() -> null).uniqueResult());
    }

    @Test
    void listObject() {
        Object object = new Object();
        assertEquals(object, new SingleResultQuery(() -> object).list().iterator().next());
    }

    @Test
    void scrollObject() {
        Object object = new Object();
        assertEquals(object, new SingleResultQuery(() -> object).scroll().iterator().next());
    }

    @Test
    void uniqueResultObject() {
        Object object = new Object();
        assertEquals(object, new SingleResultQuery(() -> object).uniqueResult());
    }

    @Test
    void setFirstResult() {
        Query query = new SingleResultQuery(null);
        assertEquals(query, query.setFirstResult(0));
    }

    @Test
    void setMaxResults() {
        Query query = new SingleResultQuery(null);
        assertEquals(query, query.setMaxResults(0));
    }

    @Test
    void setParameter() {
        Query query = new SingleResultQuery(null);
        assertEquals(query, query.setParameter("name", "value"));
    }

    @Test
    void setParameterList() {
        Query query = new SingleResultQuery(null);
        assertEquals(query, query.setParameterList("name", List.of("value")));
    }
}
