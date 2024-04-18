/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.type.ClassType;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class RequestScopeTest {
    @Test
    void testFilterQueryParams() throws Exception {
        Method method = RequestScope.class.getDeclaredMethod("getFilterParams", Map.class);
        method.setAccessible(true);

        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("foo", Collections.singletonList("bar"));
        queryParams.put("filter", Collections.singletonList("baz"));
        queryParams.put("filter[xyz]", Collections.singletonList("buzz"));
        queryParams.put("bar", Collections.singletonList("foo"));

        Map<String, List<String>> filtered = (Map<String, List<String>>) method.invoke(null, queryParams);
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("filter"));
        assertTrue(filtered.containsKey("filter[xyz]"));
    }

    @Test
    void testNewObjectsForInheritedTypes() throws Exception {
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

        Route route = Route.builder().path("/").apiVersion(NO_VERSION).build();
        ElideSettings elideSettings = ElideSettings.builder().dataStore(null).entityDictionary(dictionary).build();
        RequestScope requestScope = new RequestScope(route, null, null, UUID.randomUUID(), elideSettings, null);

        String myId = "myId";
        // Test that a new inherited class is counted for base type
        requestScope.setUUIDForObject(ClassType.of(MyInheritedClass.class), myId, new MyInheritedClass());
        assertNotNull(requestScope.getObjectById(ClassType.of(MyBaseClass.class), myId));
    }

    @Test
    void builder() {
        DataStoreTransaction dataStoreTransaction = mock(DataStoreTransaction.class);
        User user = mock(User.class);
        EntityProjection entityProjection = mock(EntityProjection.class);

        ElideSettings elideSettings = ElideSettings.builder().entityDictionary(EntityDictionary.builder().build()).build();
        Route route = Route.builder().build();
        UUID requestId = UUID.randomUUID();

        RequestScope requestScope = RequestScope.builder().route(route).requestId(requestId)
                .elideSettings(elideSettings)
                .dataStoreTransaction(dataStoreTransaction).user(user)
                .entityProjection(scope -> entityProjection)
                .build();
        assertSame(user, requestScope.getUser());
        assertSame(elideSettings, requestScope.getElideSettings());
        assertSame(route, requestScope.getRoute());
        assertSame(requestId, requestScope.getRequestId());
        assertSame(entityProjection, requestScope.getEntityProjection());
    }
}
