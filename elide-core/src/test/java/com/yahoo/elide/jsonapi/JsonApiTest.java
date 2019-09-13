/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import example.Child;
import example.Parent;
import example.TestCheckMappings;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * JSON API testing.
 */
public class JsonApiTest {
    private JsonApiMapper mapper;
    private User user = new User(0);
    private EntityDictionary dictionary;
    private DataStoreTransaction tx = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);

    @BeforeEach
    void init() {
        dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(Child.class);
        dictionary.bindInitializer(Parent::doInit, Parent.class);
        mapper = new JsonApiMapper(dictionary);
    }

    @Test
    public void checkInit() {
        // Ensure that our object receives its init before serializing
        Parent parent = new Parent();
        parent.setId(123L);
        parent.setChildren(Sets.newHashSet());
        parent.setSpouses(Sets.newHashSet());
        RequestScope userScope = new TestRequestScope(tx, user, dictionary);



        new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope).toResource();

        assertTrue(parent.init);
    }

    @Test
    public void writeSingleNoAttributesNoRel() throws JsonProcessingException {
        Parent parent = new Parent();
        parent.setId(123L);

        RequestScope userScope = new TestRequestScope(tx, user, dictionary);



        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope).toResource()));

        String expected = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":null},\"relationships\":{\"children\":{\"data\":[]},\"spouses\":{\"data\":[]}}}}";

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeSingle() throws JsonProcessingException {
        Parent parent = new Parent();
        Child child = new Child();
        parent.setId(123L);
        child.setId(2);
        parent.setChildren(Collections.singleton(child));
        parent.setFirstName("bob");
        child.setParents(Collections.singleton(parent));
        child.setFriends(new HashSet<>());

        RequestScope userScope = new TestRequestScope(tx, user, dictionary);


        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope).toResource()));

        String expected = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[]}}}}";

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeSingleIncluded() throws JsonProcessingException {
        Parent parent = new Parent();
        Child child = new Child();
        parent.setId(123L);
        child.setId(2);
        parent.setFirstName("bob");
        parent.setChildren(Collections.singleton(child));
        child.setParents(Collections.singleton(parent));
        child.setFriends(new HashSet<>());

        RequestScope userScope = new TestRequestScope(tx, user, dictionary);


        PersistentResource<Parent> pRec = new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(pRec.toResource()));
        jsonApiDocument.addIncluded(new PersistentResource<>(child,



                pRec, userScope.getUUIDFor(child), userScope).toResource());

        String expected = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[]}}},\"included\":[{\"type\":\"child\",\"id\":\"2\",\"attributes\":{\"name\":null},\"relationships\":{\"friends\":{\"data\":[]},\"parents\":{\"data\":[{\"type\":\"parent\",\"id\":\"123\"}]}}}]}";

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeList() throws JsonProcessingException {
        Parent parent = new Parent();
        Child child = new Child();
        parent.setId(123L);
        parent.setSpouses(Sets.newHashSet());
        child.setId(2);
        parent.setChildren(Collections.singleton(child));
        child.setParents(Collections.singleton(parent));
        parent.setFirstName("bob");
        child.setFriends(new HashSet<>());

        RequestScope userScope = new TestRequestScope(tx, user, dictionary);


        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(
            new Data<>(Collections.singletonList(new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope).toResource())));

        String expected = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[]}}}]}";

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeListIncluded() throws JsonProcessingException {
        Parent parent = new Parent();
        Child child = new Child();
        parent.setId(123L);
        child.setId(2);
        parent.setChildren(Collections.singleton(child));
        child.setParents(Collections.singleton(parent));
        parent.setFirstName("bob");
        child.setFriends(new HashSet<>());

        RequestScope userScope = new TestRequestScope(tx, user, dictionary);


        PersistentResource<Parent> pRec = new PersistentResource<>(parent, null, userScope.getUUIDFor(parent), userScope);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(Collections.singletonList(pRec.toResource())));
        jsonApiDocument.addIncluded(new PersistentResource<>(child,
                pRec, userScope.getUUIDFor(child), userScope).toResource());
        // duplicate will be ignored
        jsonApiDocument.addIncluded(new PersistentResource<>(child,



                pRec, userScope.getUUIDFor(child), userScope).toResource());

        String expected = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[]}}}],\"included\":[{\"type\":\"child\",\"id\":\"2\",\"attributes\":{\"name\":null},\"relationships\":{\"friends\":{\"data\":[]},\"parents\":{\"data\":[{\"type\":\"parent\",\"id\":\"123\"}]}}}]}";

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeEmptyList() throws JsonProcessingException {
        String expected = "{\"data\":[]}";

        Data<Resource> empty = new Data<>(new ArrayList<>());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(empty);

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void writeEmptyObject() throws JsonProcessingException {
        String expected = "{\"data\":null}";

        Data<Resource> empty = new Data<>((Resource) null);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(empty);

        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(expected, doc);
    }

    @Test
    public void readSingle() throws IOException {
        String doc = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":{\"type\":\"child\",\"id\":\"2\"}}}}}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> dataObj = jsonApiDocument.getData();
        Resource data = dataObj.getSingleValue();
        Map<String, Object> attributes = data.getAttributes();
        Map<String, Relationship> relations = data.getRelationships();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("child", relations.get("children").getData().getSingleValue().getType());
        assertEquals("2", relations.get("children").getData().getSingleValue().getId());
    }

    @Test
    public void readSingleWithMeta() throws IOException {
        String doc = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"data\":{\"type\":\"child\",\"id\":\"2\"}}}},\"meta\":{\"additional\":\"info\"}}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Meta meta = jsonApiDocument.getMeta();
        Data<Resource> dataObj = jsonApiDocument.getData();
        Resource data = dataObj.getSingleValue();
        Map<String, Object> attributes = data.getAttributes();
        Map<String, Relationship> relations = data.getRelationships();

        assertEquals(meta.getMetaMap().get("additional"), "info");
        assertEquals(data.getType(), "parent");
        assertEquals(data.getId(), "123");
        assertEquals(attributes.get("firstName"), "bob");
        assertEquals(relations.get("children").getData().getSingleValue().getType(), "child");
        assertEquals(relations.get("children").getData().getSingleValue().getId(), "2");
    }

    @Test
    public void readSingleIncluded() throws Exception {
        String doc = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}},\"included\":[{\"type\":\"child\",\"id\":\"2\",\"relationships\":{\"parents\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"parent\",\"id\":\"123\"}}}}]}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> dataObj = jsonApiDocument.getData();
        Resource data = dataObj.getSingleValue();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();
        Resource includedChild = included.iterator().next();
        ResourceIdentifier parent = includedChild.getRelationships()
                .get("parents")
                .getResourceIdentifierData().getSingleValue();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("child", includedChild.getType());
        assertEquals("2", includedChild.getId());
        assertEquals("123", parent.getId());
    }

    @Test
    public void readList() throws IOException {
        String doc = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}}]}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> list = jsonApiDocument.getData();
        Resource data = list.get().iterator().next();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("2", data.getRelationships().get("children").getData().getSingleValue().getId());
        assertNull(included);
    }

    @Test
    public void readListIncluded() throws IOException {
        String doc = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}}],\"included\":[{\"type\":\"child\",\"id\":\"2\",\"relationships\":{\"parents\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"parent\",\"id\":\"123\"}}}}]}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> list = jsonApiDocument.getData();
        Resource data = list.get().iterator().next();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();
        Resource includedChild = included.iterator().next();
        ResourceIdentifier parent = includedChild.getRelationships().get("parents").getResourceIdentifierData().getSingleValue();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("child", includedChild.getType());
        assertEquals("2", includedChild.getId());
        assertEquals("123", parent.getId());
    }

    @Test
    public void readListWithMeta() throws IOException {
        String doc = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}}],\"meta\":{\"additional\":\"info\"}}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Meta meta = jsonApiDocument.getMeta();
        Data<Resource> list = jsonApiDocument.getData();
        Resource data = list.get().iterator().next();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();

        assertEquals(meta.getMetaMap().get("additional"), "info");
        assertEquals(data.getType(), "parent");
        assertEquals(data.getId(), "123");
        assertEquals(attributes.get("firstName"), "bob");
        assertEquals(data.getRelationships().get("children").getData().getSingleValue().getId(), "2");
        assertNull(included);
    }
}
