/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class RequestScopeTest {
    @Test
    public void testFilterQueryParams() throws Exception {
        Method method = RequestScope.class.getDeclaredMethod("getFilterParams", MultivaluedMap.class);
        method.setAccessible(true);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("foo", Collections.singletonList("bar"));
        queryParams.put("filter", Collections.singletonList("baz"));
        queryParams.put("filter[xyz]", Collections.singletonList("buzz"));
        queryParams.put("bar", Collections.singletonList("foo"));

        MultivaluedMap<String, String> filtered = (MultivaluedMap<String, String>) method.invoke(null, queryParams);
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("filter"));
        assertTrue(filtered.containsKey("filter[xyz]"));
    }

    @Test
    public void testNewObjectsForInheritedTypes() throws Exception {
        // NOTE: This tests that inherited types are properly accounted for during a patch extension request.
        //       otherwise it is possible to create an inherited type and when performing introspection on a
        //       superclass we will miss that the object is newly created and then we'll attempt to query
        //       the datastore.

        @Entity
        @Include(rootLevel = false)
        class MyBaseClass {
            @Id
            public long id;
        }

        @Entity
        @Include(rootLevel = false)
        class MyInheritedClass extends MyBaseClass {
            public String myField;
        }

        EntityDictionary dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(MyBaseClass.class);
        dictionary.bindEntity(MyInheritedClass.class);

        RequestScope requestScope = new RequestScope(null, "/", NO_VERSION, null, null, null, null, null, UUID.randomUUID(),
                new ElideSettingsBuilder(null)
                        .withEntityDictionary(dictionary)
                        .build());

        String myId = "myId";
        // Test that a new inherited class is counted for base type
        requestScope.setUUIDForObject(ClassType.of(MyInheritedClass.class), myId, new MyInheritedClass());
        assertNotNull(requestScope.getObjectById(ClassType.of(MyBaseClass.class), myId));
    }
}
