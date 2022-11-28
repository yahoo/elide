/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TestRequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Child;
import example.Parent;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private User user = new TestUser("0");
    private static String BASE_URL = "http://localhost:8080";

    private EntityDictionary dictionary;
    private DataStoreTransaction tx = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);

    @BeforeEach
    void init() {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(Child.class);
        mapper = new JsonApiMapper();
    }

    @Test
    public void writeSingleNoAttributesNoRel() throws JsonProcessingException {
        Parent parent = new Parent();
        parent.setId(123L);

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(new PersistentResource<>(parent, userScope.getUUIDFor(parent), userScope).toResource()));

        String expected = "{\"data\":{"
                + "\"type\":\"parent\","
                + "\"id\":\"123\","
                + "\"attributes\":{\"firstName\":null},"
                + "\"relationships\":{"
                +   "\"children\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/children\",\"related\":\"http://localhost:8080/json/parent/123/children\"},"
                +       "\"data\":[]},"
                +   "\"spouses\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/spouses\",\"related\":\"http://localhost:8080/json/parent/123/spouses\"},"
                +       "\"data\":[]}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/parent/123\"}}}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
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

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(new PersistentResource<>(parent, userScope.getUUIDFor(parent), userScope).toResource()));

        String expected = "{\"data\":{"
                + "\"type\":\"parent\","
                + "\"id\":\"123\","
                + "\"attributes\":{\"firstName\":\"bob\"},"
                + "\"relationships\":{"
                +   "\"children\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/children\",\"related\":\"http://localhost:8080/json/parent/123/children\"},"
                +       "\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},"
                +   "\"spouses\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/spouses\",\"related\":\"http://localhost:8080/json/parent/123/spouses\"},"
                +       "\"data\":[]}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/parent/123\"}}}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
    }

    @Test
    public void writeSingleWithMeta() throws JsonProcessingException {
        Child child = new Child();
        child.setId(2);
        child.setMetadataField("foo", "bar");

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(new PersistentResource<>(child, userScope.getUUIDFor(child), userScope).toResource()));

        String expected = "{\"data\":{\"type\":\"child\",\"id\":\"2\",\""
                + "links\":{\"self\":\"http://localhost:8080/json/child/2\"},\"meta\":{\"foo\":\"bar\"}}}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

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

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        PersistentResource<Parent> pRec = new PersistentResource<>(parent, userScope.getUUIDFor(parent), userScope);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(pRec.toResource()));
        jsonApiDocument.addIncluded(
                new PersistentResource<>(child, pRec, "children", userScope.getUUIDFor(child), userScope).toResource());

        String expected = "{\"data\":{"
                + "\"type\":\"parent\","
                + "\"id\":\"123\","
                + "\"attributes\":{\"firstName\":\"bob\"},"
                + "\"relationships\":{"
                +   "\"children\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/children\",\"related\":\"http://localhost:8080/json/parent/123/children\"},"
                +       "\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},"
                +   "\"spouses\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/spouses\",\"related\":\"http://localhost:8080/json/parent/123/spouses\"},"
                +       "\"data\":[]}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/parent/123\"}},"
                + "\"included\":[{"
                +   "\"type\":\"child\","
                +   "\"id\":\"2\","
                +   "\"attributes\":{\"name\":null},"
                +   "\"relationships\":{"
                +       "\"friends\":{"
                +           "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2/relationships/friends\",\"related\":\"http://localhost:8080/json/parent/123/children/2/friends\"},"
                +           "\"data\":[]},"
                +       "\"parents\":{"
                +           "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2/relationships/parents\",\"related\":\"http://localhost:8080/json/parent/123/children/2/parents\"},"
                +           "\"data\":[{\"type\":\"parent\",\"id\":\"123\"}]}},"
                +   "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2\"}}]}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
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

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(
            new Data<>(Collections.singletonList(new PersistentResource<>(parent, userScope.getUUIDFor(parent), userScope).toResource())));

        String expected = "{\"data\":[{"
                + "\"type\":\"parent\","
                + "\"id\":\"123\","
                + "\"attributes\":{\"firstName\":\"bob\"},"
                + "\"relationships\":{"
                +   "\"children\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/children\",\"related\":\"http://localhost:8080/json/parent/123/children\"},"
                +       "\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},"
                +   "\"spouses\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/spouses\",\"related\":\"http://localhost:8080/json/parent/123/spouses\"},"
                +       "\"data\":[]}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/parent/123\"}}]}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
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

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        PersistentResource<Parent> pRec = new PersistentResource<>(parent, userScope.getUUIDFor(parent), userScope);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(Collections.singletonList(pRec.toResource())));
        jsonApiDocument.addIncluded(new PersistentResource<>(child,
                pRec, "children", userScope.getUUIDFor(child), userScope).toResource());
        // duplicate will be ignored
        jsonApiDocument.addIncluded(
                new PersistentResource<>(child, pRec, "children", userScope.getUUIDFor(child), userScope).toResource());

        String expected = "{\"data\":[{"
                + "\"type\":\"parent\","
                + "\"id\":\"123\","
                + "\"attributes\":{\"firstName\":\"bob\"},"
                + "\"relationships\":{"
                +   "\"children\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/children\",\"related\":\"http://localhost:8080/json/parent/123/children\"},"
                +       "\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},"
                +   "\"spouses\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/relationships/spouses\",\"related\":\"http://localhost:8080/json/parent/123/spouses\"},"
                +       "\"data\":[]}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/parent/123\"}}],"
                + "\"included\":[{"
                +   "\"type\":\"child\","
                +   "\"id\":\"2\","
                +   "\"attributes\":{\"name\":null},"
                +   "\"relationships\":{"
                +       "\"friends\":{"
                +           "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2/relationships/friends\",\"related\":\"http://localhost:8080/json/parent/123/children/2/friends\"},"
                +           "\"data\":[]},"
                +       "\"parents\":{"
                +           "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2/relationships/parents\",\"related\":\"http://localhost:8080/json/parent/123/children/2/parents\"},"
                +           "\"data\":[{\"type\":\"parent\",\"id\":\"123\"}]}},"
                +   "\"links\":{\"self\":\"http://localhost:8080/json/parent/123/children/2\"}}]"
                + "}";

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
    }

    @Test
    public void writeEmptyList() throws JsonProcessingException {
        String expected = "{\"data\":[]}";

        Data<Resource> empty = new Data<>(new ArrayList<>());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(empty);

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
    }

    @Test
    public void writeEmptyObject() throws JsonProcessingException {
        String expected = "{\"data\":null}";

        Data<Resource> empty = new Data<>((Resource) null);

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(empty);

        Data<Resource> data = jsonApiDocument.getData();
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertEquals(data, jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
    }

    @Test
    public void writeNullObject() throws JsonProcessingException {
        String expected = "{\"data\":null}";

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(null);

        assertNull(jsonApiDocument.getData());
        String doc = mapper.writeJsonApiDocument(jsonApiDocument);
        assertNull(jsonApiDocument.getData());

        assertEquals(expected, doc);
        checkEquality(jsonApiDocument);
    }

    @Test
    public void testMissingTypeInResource() {
        String doc = "{ \"data\": { \"id\": \"22\", \"attributes\": { \"title\": \"works fine\" } } }";

        assertThrows(JsonMappingException.class, () -> mapper.readJsonApiDocument(doc));
    }

    @Test
    public void testMissingTypeInResourceList() {
        String doc = "{ \"data\": [{ \"id\": \"22\", \"attributes\": { \"title\": \"works fine\" } } ]}";

        assertThrows(JsonMappingException.class, () -> mapper.readJsonApiDocument(doc));
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
        checkEquality(jsonApiDocument);
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
        checkEquality(jsonApiDocument);
    }

    @Test
    public void readSingleIncluded() throws Exception {
        String doc = "{\"data\":{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}},\"included\":[{\"type\":\"child\",\"id\":\"2\",\"relationships\":{\"parents\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"parent\",\"id\":\"123\"}}}}]}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> dataObj = jsonApiDocument.getData();
        Resource data = dataObj.getSingleValue();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();
        Resource includedChild = IterableUtils.first(included);
        ResourceIdentifier parent = includedChild.getRelationships()
                .get("parents")
                .getResourceIdentifierData().getSingleValue();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("child", includedChild.getType());
        assertEquals("2", includedChild.getId());
        assertEquals("123", parent.getId());
        checkEquality(jsonApiDocument);
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
        checkEquality(jsonApiDocument);
    }

    @Test
    public void readListIncluded() throws IOException {
        String doc = "{\"data\":[{\"type\":\"parent\",\"id\":\"123\",\"attributes\":{\"firstName\":\"bob\"},\"relationships\":{\"children\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"child\",\"id\":\"2\"}}}}],\"included\":[{\"type\":\"child\",\"id\":\"2\",\"relationships\":{\"parents\":{\"links\":{\"self\":\"/parent/123/relationships/child\",\"related\":\"/parent/123/child\"},\"data\":{\"type\":\"parent\",\"id\":\"123\"}}}}]}";

        JsonApiDocument jsonApiDocument = mapper.readJsonApiDocument(doc);

        Data<Resource> list = jsonApiDocument.getData();
        Resource data = list.get().iterator().next();
        Map<String, Object> attributes = data.getAttributes();
        List<Resource> included = jsonApiDocument.getIncluded();
        Resource includedChild = IterableUtils.first(included);
        ResourceIdentifier parent = includedChild.getRelationships().get("parents").getResourceIdentifierData().getSingleValue();

        assertEquals("parent", data.getType());
        assertEquals("123", data.getId());
        assertEquals("bob", attributes.get("firstName"));
        assertEquals("child", includedChild.getType());
        assertEquals("2", includedChild.getId());
        assertEquals("123", parent.getId());
        checkEquality(jsonApiDocument);
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
        checkEquality(jsonApiDocument);
    }

    @Test
    public void compareNullAndEmpty() {
        Data<Resource> empty = new Data<>((Resource) null);

        JsonApiDocument jsonApiEmpty = new JsonApiDocument();
        jsonApiEmpty.setData(empty);

        JsonApiDocument jsonApiNull = new JsonApiDocument();
        jsonApiNull.setData(null);

        assertEquals(jsonApiEmpty, jsonApiNull);
        assertEquals(jsonApiEmpty.hashCode(), jsonApiNull.hashCode());
    }

    @Test
    public void compareOrder() {
        Parent parent1 = new Parent();
        parent1.setId(123L);
        Parent parent2 = new Parent();
        parent2.setId(456L);

        RequestScope userScope = new TestRequestScope(BASE_URL, tx, user, dictionary);

        PersistentResource<Parent> pRec1 = new PersistentResource<>(parent1, userScope.getUUIDFor(parent1), userScope);
        PersistentResource<Parent> pRec2 = new PersistentResource<>(parent2, userScope.getUUIDFor(parent2), userScope);

        JsonApiDocument jsonApiDocument1 = new JsonApiDocument();
        jsonApiDocument1.setData(new Data<>(Lists.newArrayList(pRec1.toResource(), pRec2.toResource())));

        JsonApiDocument jsonApiDocument2 = new JsonApiDocument();
        jsonApiDocument2.setData(new Data<>(Lists.newArrayList(pRec2.toResource(), pRec1.toResource())));

        assertEquals(jsonApiDocument1, jsonApiDocument2);
        assertEquals(jsonApiDocument1.hashCode(), jsonApiDocument2.hashCode());

        jsonApiDocument1.getData().sort((a, b) -> Integer.compare(a.hashCode(), b.hashCode()));
        jsonApiDocument2.getData().sort((a, b) -> Integer.compare(b.hashCode(), a.hashCode()));

        assertEquals(jsonApiDocument1, jsonApiDocument2);
        assertEquals(jsonApiDocument1.hashCode(), jsonApiDocument2.hashCode());
    }

    private void checkEquality(JsonApiDocument doc1) {
        JsonApiDocument doc2;
        try {
            String json = mapper.writeJsonApiDocument(doc1);
            doc2 = mapper.readJsonApiDocument(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertEquals(doc1, doc2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
    }
}
