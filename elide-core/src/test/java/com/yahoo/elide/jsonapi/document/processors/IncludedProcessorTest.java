/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.security.User;

import com.google.common.collect.Sets;

import example.Child;
import example.FunWithPermissions;
import example.Parent;
import example.TestCheckMappings;

import org.mockito.Answers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    @BeforeMethod
    public void setUp() throws Exception {
        includedProcessor = new IncludedProcessor();

        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(FunWithPermissions.class);

        ElideSettings elideSettings = new ElideSettingsBuilder(null)
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(dictionary)
                .build();

        RequestScope goodUserScope = new RequestScope(null,
                new JsonApiDocument(), mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS),
                new User(1), null,
                elideSettings, false);

        RequestScope badUserScope = new RequestScope(null,
                new JsonApiDocument(), mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS),
                new User(-1), null,
                elideSettings, false);

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

        //Create Persistent Resources
        parentRecord1 = new PersistentResource<>(parent1, null, goodUserScope.getUUIDFor(parent1), goodUserScope);
        parentRecord2 = new PersistentResource<>(parent2, null, goodUserScope.getUUIDFor(parent2), goodUserScope);
        parentRecord3 = new PersistentResource<>(parent3, null, goodUserScope.getUUIDFor(parent3), goodUserScope);
        childRecord1  = new PersistentResource<>(child1, null, goodUserScope.getUUIDFor(child1), goodUserScope);
        childRecord2  = new PersistentResource<>(child2, null, goodUserScope.getUUIDFor(child2), goodUserScope);
        childRecord3  = new PersistentResource<>(child3, null, goodUserScope.getUUIDFor(child3), goodUserScope);
        childRecord4  = new PersistentResource<>(child4, null, goodUserScope.getUUIDFor(child4), goodUserScope);

        funWithPermissionsRecord = new PersistentResource<>(funWithPermissions, null, goodUserScope.getUUIDFor(funWithPermissions), badUserScope);
    }

    @Test
    public void testExecuteSingleRelation() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children"));
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded = Collections.singletonList(childRecord1.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        Assert.assertEquals(actualIncluded, expectedIncluded,
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
        includedProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));

        List<Resource> expectedIncluded = Arrays.asList(childRecord1.toResource(), childRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        Assert.assertEquals(actualIncluded, expectedIncluded,
                "Included Processor added requested resource from all records");
    }

    @Test
    public void testExecuteSingleNestedRelation() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children.friends"));
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded =
                Arrays.asList(childRecord1.toResource(), childRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        Assert.assertEquals(actualIncluded, expectedIncluded,
                "Included Processor added single nested requested resources from 'include' query param");
    }

    @Test
    public void testExecuteMultipleRelations() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Arrays.asList("children", "spouses"));
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        List<Resource> expectedIncluded =
                Arrays.asList(childRecord1.toResource(), parentRecord2.toResource());
        List<Resource> actualIncluded = jsonApiDocument.getIncluded();

        Assert.assertEquals(actualIncluded, expectedIncluded,
                "Included Processor added single requested resource from 'include' query param");
    }

    @Test
    public void testExecuteMultipleNestedRelations() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("children.friends"));
        includedProcessor.execute(jsonApiDocument, parentRecord3, Optional.of(queryParams));

        Set<Resource> expectedIncluded =
                Sets.newHashSet(
                        childRecord1.toResource(),
                        childRecord2.toResource(),
                        childRecord3.toResource(),
                        childRecord4.toResource()
                );
        Set<Resource> actualIncluded = new HashSet<>(jsonApiDocument.getIncluded());

        Assert.assertEquals(actualIncluded, expectedIncluded,
                "Included Processor added multiple nested requested resource collections from 'include' query param");
    }

    @Test
    public void testIncludeForbiddenRelationship() {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(INCLUDE, Collections.singletonList("relation1"));
        includedProcessor.execute(jsonApiDocument, funWithPermissionsRecord, Optional.of(queryParams));

        Assert.assertNull(jsonApiDocument.getIncluded(),
                "Included Processor included forbidden relationship");
    }

    @Test
    public void testNoQueryParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.empty());

        Assert.assertNull(jsonApiDocument.getIncluded(),
                "Included Processor adds no resources when not given query params");
    }


    @Test
    public void testNoQueryIncludeParams() throws Exception {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("unused", Collections.emptyList());
        includedProcessor.execute(jsonApiDocument, parentRecord1, Optional.of(queryParams));

        Assert.assertNull(jsonApiDocument.getIncluded(),
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
