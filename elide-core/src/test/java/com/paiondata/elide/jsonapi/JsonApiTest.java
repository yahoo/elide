/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TestRequestScope;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.lifecycle.FieldTestModel;
import com.paiondata.elide.core.lifecycle.LegacyTestModel;
import com.paiondata.elide.core.lifecycle.PropertyTestModel;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.TestUser;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.jsonapi.models.Data;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.JsonApiError;
import com.paiondata.elide.jsonapi.models.JsonApiErrors;
import com.paiondata.elide.jsonapi.models.Meta;
import com.paiondata.elide.jsonapi.models.Relationship;
import com.paiondata.elide.jsonapi.models.Resource;
import com.paiondata.elide.jsonapi.models.ResourceIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import example.Child;
import example.Parent;

import org.apache.commons.collections4.IterableUtils;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.skyscreamer.jsonassert.JSONAssert;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * JSON API testing.
 */
public class JsonApiTest {
    private JsonApiMapper mapper;
    private User user = new TestUser("0");
    private static String BASE_URL = "http://localhost:8080/json";

    private EntityDictionary dictionary;
    private DataStoreTransaction tx = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);

    @BeforeEach
    void init() {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
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

        try {
            JSONAssert.assertEquals(expected, doc, true);
        } catch (JSONException e) {
            fail(e);
        }
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

        try {
            JSONAssert.assertEquals(expected, doc, true);
        } catch (JSONException e) {
            fail(e);
        }
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

    @Test
    void constraintViolationException() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);

        String body = """
                {"data": {"type":"testModel","id":"1","attributes": {"field":"Foo"}}}""";

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        TestObject testObject = new TestObject();
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        ConstraintViolationException e = new ConstraintViolationException("message", violations);

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(e).when(tx).preCommit(any());

        Route route = Route.builder().baseUrl(BASE_URL).path("/testModel").build();
        ElideResponse<String> response = new JsonApi(elide).post(route, body, null, UUID.randomUUID());
        JsonApiErrors errorObjects = mapper.getObjectMapper().readValue(response.getBody(), JsonApiErrors.class);
        assertEquals(3, errorObjects.getErrors().size());
        for (JsonApiError errorObject : errorObjects.getErrors()) {
            Map<String, Object> meta = errorObject.getMeta();
            String expected;
            String actual = mapper.getObjectMapper().writeValueAsString(errorObject);
            switch (meta.get("property").toString()) {
            case "nestedTestObject.nestedNotNullField":
                expected = """
                        {"code":"NotNull","source":{"pointer":"/data/attributes/nestedTestObject/nestedNotNullField"},"detail":"must not be null","meta":{"type":"ConstraintViolation","property":"nestedTestObject.nestedNotNullField"}}""";
                assertEquals(expected, actual);
                break;
            case "notNullField":
                expected = """
                        {"code":"NotNull","source":{"pointer":"/data/attributes/notNullField"},"detail":"must not be null","meta":{"type":"ConstraintViolation","property":"notNullField"}}""";
                assertEquals(expected, actual);
                break;
            case "minField":
                expected = """
                        {"code":"Min","source":{"pointer":"/data/attributes/minField"},"detail":"must be greater than or equal to 5","meta":{"type":"ConstraintViolation","property":"minField"}}""";
                assertEquals(expected, actual);
                break;
            }
        }

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, exceptionMappers);
        return new Elide(settings, new TransactionRegistry(), settings.getEntityDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        return ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .verboseErrors(true)
                .settings(JsonApiSettings.builder().jsonApiExceptionHandler(new DefaultJsonApiExceptionHandler(
                        new Slf4jExceptionLogger(), exceptionMappers, new DefaultJsonApiErrorMapper())))
                .build();
    }

    public static class TestObject {
        public static class NestedTestObject {
            @NotNull
            private String nestedNotNullField;
        }

        @NotNull
        private String notNullField;

        @Min(5)
        private int minField = 1;

        @Valid
        private NestedTestObject nestedTestObject = new NestedTestObject();
    }
}
