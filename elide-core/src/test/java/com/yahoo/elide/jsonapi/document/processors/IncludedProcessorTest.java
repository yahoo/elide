/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;


import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.security.User;

import com.google.common.collect.Sets;
import example.Child;
import example.FunWithPermissions;
import example.Parent;
import example.TestCheckMappings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


public class IncludedProcessorTest {
    private static final String INCLUDE = "include";

    private IncludedProcessor includedProcessor;

    private PersistentResource<Parent> parentRecord1;
    private PersistentResource<Parent> parentRecord2;
    private PersistentResource<Parent> parentRecord3;

    private PersistentResource<Child> childRecord1;
    private PersistentResource<Child> childRecord2;
    private PersistentResource<Child> childRecord3;
    private PersistentResource<Child> childRecord4;

    private PersistentResource<FunWithPermissions> funWithPermissionsRecord;

    private DataStoreTransaction mockTransaction = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);
    private TestRequestScope testScope;
    private EntityDictionary dictionary;



    @BeforeEach
    public void setUp() throws Exception {
        includedProcessor = new IncludedProcessor();

        dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(FunWithPermissions.class);

        reset(mockTransaction);

        testScope = new TestRequestScope(mockTransaction, new User(1), dictionary);


    }

    @Test
    public void testExecuteSingleRelation() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded = Collections.singletonList(childRecord1.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added single requested resource from 'include' query param");
    }

    @Test
    public void testExecuteSingleRelationOnCollection() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        Set<PersistentResource> parents = new HashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));

        List<Resource> expectedIncluded = Arrays.asList(childRecord1.toResource(), childRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added requested resource from all records");
    }

    @Test
    public void testExecuteSingleNestedRelation() throws Exception {

        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children.friends"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded =
                Arrays.asList(childRecord1.toResource(), childRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added single nested requested resources from 'include' query param");
    }

    @Test
    public void testExecuteMultipleRelations() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Arrays.asList("children", "spouses"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded =
                Arrays.asList(childRecord1.toResource(), parentRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added single requested resource from 'include' query param");
    }

    @Test
    public void testExecuteMultipleNestedRelations() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children.friends"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parentRecord3, Optional.of(queryParams));

        Set<Resource> expectedIncluded =
                Sets.newHashSet(
                        childRecord1.toResource(),
                        childRecord2.toResource(),
                        childRecord3.toResource(),
                        childRecord4.toResource()
                );
        Set<Resource> actualIncluded = new HashSet<>(jsonApiDocument.getIncluded());

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added multiple nested requested resource collections from 'include' query param");
    }

    @Test
    public void testIncludeForbiddenRelationship() {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("relation1"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, funWithPermissionsRecord, Optional.of(queryParams));

        assertNull(jsonApiDocument.getIncluded(),
                "Included Processor included forbidden relationship");
    }

    @Test
    public void testNoQueryParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.empty());

        assertNull(jsonApiDocument.getIncluded(),
                "Included Processor adds no resources when not given query params");
    }


    @Test
    public void testNoQueryIncludeParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("unused", Collections.emptyList());
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        assertNull(jsonApiDocument.getIncluded(),
                "Included Processor adds no resources when not given query params");
    }

    private static Parent newParent(int id) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        return parent;
    }

    private static Child newChild(int id) {
        Child child = new Child();
        child.setId(id);
        child.setParents(new HashSet<>());
        return child;
    }

    private FunWithPermissions newFunWithPermissions(int id) {
        FunWithPermissions funWithPermissions = new FunWithPermissions();
        funWithPermissions.setId(id);
        funWithPermissions.setRelation1(new HashSet<>());
        return funWithPermissions;
    }
}
