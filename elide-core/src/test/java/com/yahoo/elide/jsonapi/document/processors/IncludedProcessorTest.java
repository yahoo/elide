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

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.google.common.collect.Sets;
import example.Child;
import example.FunWithPermissions;
import example.Parent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(FunWithPermissions.class);

        reset(mockTransaction);

        testScope = new TestRequestScope(mockTransaction, new TestUser("1"), dictionary);

        //Create objects
        Parent parent1 = newParent(1);
        Parent parent2 = newParent(2);
        Parent parent3 = newParent(3);

        Child child1 = newChild(2);
        Child child2 = newChild(3);
        Child child3 = newChild(4);
        Child child4 = newChild(5);

        FunWithPermissions funWithPermissions = newFunWithPermissions(1);

        //Form relationships
        parent1.setSpouses(new HashSet<>(Collections.singletonList(parent2)));
        parent1.setChildren(new HashSet<>(Collections.singletonList(child1)));
        parent2.setChildren(new HashSet<>(Collections.singletonList(child2)));
        child1.setFriends(new HashSet<>(Collections.singletonList(child2)));

        //Parent with multiple children each with a friend
        parent3.setChildren(new HashSet<>(Arrays.asList(child3, child4)));
        child3.setFriends(new HashSet<>(Collections.singletonList(child1)));
        child4.setFriends(new HashSet<>(Collections.singletonList(child2)));

        //Create Persistent Resource
        parentRecord1 = new PersistentResource<>(parent1, String.valueOf(parent1.getId()), testScope);
        parentRecord2 = new PersistentResource<>(parent2, String.valueOf(parent2.getId()), testScope);
        parentRecord3 = new PersistentResource<>(parent3, String.valueOf(parent3.getId()), testScope);
        childRecord1  = new PersistentResource<>(child1, String.valueOf(child1.getId()), testScope);
        childRecord2  = new PersistentResource<>(child2, String.valueOf(child2.getId()), testScope);
        childRecord3  = new PersistentResource<>(child3, String.valueOf(child3.getId()), testScope);
        childRecord4  = new PersistentResource<>(child4, String.valueOf(child4.getId()), testScope);

        funWithPermissionsRecord = new PersistentResource<>(funWithPermissions,
                String.valueOf(funWithPermissions.getId()), testScope);
    }

    @Test
    public void testExecuteSingleRelation() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord1, queryParams);

        List<Resource> expectedIncluded = Collections.singletonList(childRecord1.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        assertEquals(expectedIncluded, actualIncluded,
                "Included Processor added single requested resource from 'include' query param");
    }

    @Test
    public void testExecuteSingleRelationOnCollection() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        LinkedHashSet<PersistentResource> parents = new LinkedHashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children"));
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, testScope, parents, queryParams);

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
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord1, queryParams);

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
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord1, queryParams);

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
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord3, queryParams);

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
        includedProcessor.execute(jsonApiDocument, testScope, funWithPermissionsRecord, queryParams);

        assertNull(jsonApiDocument.getIncluded(),
                "Included Processor included forbidden relationship");
    }

    @Test
    public void testNoQueryParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord1, null);

        assertNull(jsonApiDocument.getIncluded(),
                "Included Processor adds no resources when not given query params");
    }


    @Test
    public void testNoQueryIncludeParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("unused", Collections.emptyList());
        testScope.setQueryParams(queryParams);
        includedProcessor.execute(jsonApiDocument, testScope, parentRecord1, queryParams);

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
