/*
 * Copyright 2025, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.jsonapi.example.Author;
import com.yahoo.elide.jsonapi.example.Book;
import com.yahoo.elide.jsonapi.example.FunWithPermissions;
import com.yahoo.elide.jsonapi.example.Parent;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tests {@link JsonApiPersistentResource} and JSON-API-specific {@link RequestScope} behavior: relationship
 * document parsing, permission-filtered serialization, JSON Patch scopes, and RSQL filter query-param parsing.
 */
public class JsonApiPersistentResourceTest {

    private final User goodUser = new User(() -> "1");
    private final User badUser = new User(() -> "-1");

    private final DataStoreTransaction tx = mock(DataStoreTransaction.class);

    private final EntityDictionary dictionary = buildDictionary();

    private final ElideSettings elideSettings = ElideSettings.builder().dataStore(null)
            .entityDictionary(dictionary)
            .build();

    /* Unlike elideSettings above, this registers JsonApiSettings, which JsonApiRequestScope (and its subclasses)
     * require to be present. */
    private final ElideSettings jsonApiElideSettings = ElideSettings.builder().dataStore(null)
            .entityDictionary(dictionary)
            .settings(JsonApiSettings.builder())
            .build();

    private static EntityDictionary buildDictionary() {
        EntityDictionary dictionary = EntityDictionary.builder()
                .checks(Map.of("goodUser", FunWithPermissions.GoodUserCheck.class))
                .build();
        dictionary.bindEntity(FunWithPermissions.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        return dictionary;
    }

    @BeforeEach
    public void beforeTest() {
        reset(tx);
    }

    private RequestScope buildRequestScope(User user) {
        Route route = Route.builder().apiVersion(NO_VERSION).build();
        return RequestScope.builder().route(route).dataStoreTransaction(tx).user(user)
                .requestId(UUID.randomUUID()).elideSettings(elideSettings).build();
    }

    private JsonApiRequestScope buildJsonApiRequestScope(String path, DataStoreTransaction tx, User user,
            Map<String, List<String>> queryParams) {
        Route route = Route.builder().path(path).apiVersion(NO_VERSION).parameters(queryParams).build();
        return JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx).user(user)
                .requestId(UUID.randomUUID()).jsonApiDocument(new JsonApiDocument())
                .elideSettings(jsonApiElideSettings).build();
    }

    private static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions entity = new FunWithPermissions();

        PersistentResource<FunWithPermissions> resource = new PersistentResource<>(entity, "3", buildRequestScope(goodUser));

        Map<String, Relationship> relationships = JsonApiPersistentResource.getRelationships(resource);

        assertEquals(2, relationships.size(), "Good user should see both relationships.");
        assertTrue(relationships.containsKey("openRelation"), "openRelation should be present");
        assertTrue(relationships.containsKey("restrictedRelation"), "restrictedRelation should be present");

        PersistentResource<FunWithPermissions> badResource =
                new PersistentResource<>(entity, "3", buildRequestScope(badUser));
        relationships = JsonApiPersistentResource.getRelationships(badResource);

        assertEquals(1, relationships.size(), "Bad user should only see the open relationship.");
        assertTrue(relationships.containsKey("openRelation"), "openRelation should be present");
    }

    @Test
    public void testGetAttributes() {
        when(tx.getAttribute(any(), any(), any())).thenCallRealMethod();

        FunWithPermissions entity = new FunWithPermissions();
        entity.setOpenField("open");
        entity.setRestrictedField("restricted");

        PersistentResource<FunWithPermissions> resource = new PersistentResource<>(entity, "3", buildRequestScope(goodUser));

        Map<String, Object> attributes = JsonApiPersistentResource.getAttributes(resource);

        assertEquals(2, attributes.size(), "Good user should see both attributes.");
        assertEquals("open", attributes.get("openField"));
        assertEquals("restricted", attributes.get("restrictedField"));

        PersistentResource<FunWithPermissions> badResource =
                new PersistentResource<>(entity, "3", buildRequestScope(badUser));
        attributes = JsonApiPersistentResource.getAttributes(badResource);

        assertEquals(1, attributes.size(), "Bad user should only see the open attribute.");
        assertEquals("open", attributes.get("openField"));
    }

    /**
     * Verify that Relationship toMany cannot contain null resources, but toOne can.
     *
     * @throws Exception
     */
    @Test
    public void testRelationshipMissingData() throws Exception {
        RequestScope goodScope = buildRequestScope(goodUser);

        // null resource in toMany relationship is not valid
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        idList.add(null);
        assertThrows(
                NullPointerException.class,
                () -> new Relationship(Collections.emptyMap(), new Data<>(idList)));

        // However null toOne relationship is valid
        Relationship toOneRelationship = new Relationship(Collections.emptyMap(), new Data<>((Resource) null));
        assertTrue(toOneRelationship.getData().get().isEmpty());
        assertNull(toOneRelationship.toPersistentResources(goodScope));

        // no Data
        Relationship nullRelationship = new Relationship(Collections.emptyMap(), null);
        assertNull(nullRelationship.getData());
        assertNull(nullRelationship.toPersistentResources(goodScope));
    }

    @Test
    public void testPatchRequestScope() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Route route = Route.builder().path("/book").apiVersion(NO_VERSION).build();
        JsonApiJsonPatchRequestScope parentScope = new JsonApiJsonPatchRequestScope(
                route,
                tx,
                goodUser,
                UUID.randomUUID(),
                jsonApiElideSettings);
        JsonApiJsonPatchRequestScope scope = new JsonApiJsonPatchRequestScope(
                parentScope.getRoute().getPath(), parentScope.getJsonApiDocument(), parentScope);
        // verify wrap works
        assertEquals(parentScope.getUpdateStatusCode(), scope.getUpdateStatusCode());
        assertEquals(parentScope.getObjectEntityCache(), scope.getObjectEntityCache());

        Parent parent = new Parent();
        parent.setId(7);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", scope);
        parentResource.updateAttribute("firstName", "foobar");

        ArgumentCaptor<Attribute> attributeArgument = ArgumentCaptor.forClass(Attribute.class);
        verify(tx, times(1)).setAttribute(eq(parent), attributeArgument.capture(), eq(scope));
        assertEquals(attributeArgument.getValue().getName(), "firstName");
        assertEquals(attributeArgument.getValue().getArguments().iterator().next().getValue(), "foobar");
    }

    @Test
    public void testFilterExpressionByType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.name][infix]",
                "Hemingway"
        );

        JsonApiRequestScope scope = buildJsonApiRequestScope("/", mock(DataStoreTransaction.class), goodUser,
                queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Author.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Author].name", predicate.getPath().toString());
    }

    @Test
    public void testFilterExpressionCollection() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.authors.name][infix]",
                "Hemingway"
        );

        JsonApiRequestScope scope = buildJsonApiRequestScope("/", mock(DataStoreTransaction.class), goodUser,
                queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Book.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("authors.name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Book].authors/[Author].name", predicate.getPath().toString());
    }
}
