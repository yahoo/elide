/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.swagger.model.Resource;
import com.google.common.collect.ImmutableMap;
import example.models.Author;
import example.models.Book;
import example.models.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.StringProperty;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonApiModelResolverTest {

    private static final String KEY_BOOK = "book";
    private static final String KEY_PUBLISHER = "publisher";
    private static final String KEY_AUTHOR = "author";

    private static final Map<String, Class> ENTITIES =
        ImmutableMap.of(KEY_BOOK, Book.class,
                        KEY_PUBLISHER, Publisher.class,
                        KEY_AUTHOR, Author.class);

    private ModelConverters converters;

    @BeforeAll
    public void setup() {
        EntityDictionary dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(ENTITIES.get(KEY_BOOK));
        dictionary.bindEntity(ENTITIES.get(KEY_PUBLISHER));
        dictionary.bindEntity(ENTITIES.get(KEY_AUTHOR));

        converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));
    }

    @Test
    public void testBookPermissions() {
        StringProperty entity = getStringProperty(KEY_BOOK, "type");
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");
        ObjectProperty relationships = getObjectProperty(KEY_BOOK, "relationships");

        String entityPermissions = entity.getDescription();
        assertEquals("Create Permissions : (Principal is author)\nDelete Permissions : (Prefab.Role.None)",
                entityPermissions);

        String titlePermissions = attributes.getProperties().get("title").getDescription();
        assertEquals("Read Permissions : (Principal is author OR Principal is publisher)", titlePermissions);

        String publisherPermissions = relationships.getProperties().get("publisher").getDescription();
        assertEquals("Read Permissions : (Principal is author OR Principal is publisher)\n"
                + "Update Permissions : (Principal is publisher)",
                publisherPermissions);
    }

    @Test
    public void testModelDescriptions() {
        Resource model = getModel(KEY_BOOK);
        assertEquals("A book", model.getDescription());

        model = getModel(KEY_AUTHOR);
        assertEquals("The Author", model.getDescription());

        model = getModel(KEY_PUBLISHER);
        assertNull(model.getDescription());
    }

    @Test
    public void testModelResolution() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");
        ObjectProperty relationships = getObjectProperty(KEY_PUBLISHER, "relationships");

        assertEquals(3, attributes.getProperties().size());
        assertEquals(2, relationships.getProperties().size());
        assertTrue(attributes.getProperties().containsKey("billingAddress"));
        assertTrue(attributes.getProperties().containsKey("billingCodes"));
        assertTrue(relationships.getProperties().containsKey("books"));
        assertTrue(relationships.getProperties().containsKey("exclusiveAuthors"));
    }

    @Test
    public void testDescription() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");

        String phoneDescription = attributes.getProperties().get("phone").getDescription();
        assertEquals("Phone number", phoneDescription);
    }

    @Test
    public void testNoDescriptionWithoutAnnotation() {
        ObjectProperty attributes = getObjectProperty(KEY_AUTHOR, "attributes");

        Object nameDescription = attributes.getProperties().get("name").getDescription();
        assertNull(nameDescription);
    }

    @Test
    public void testNoDescriptionWhenNotProvided() {
        ObjectProperty attributes = getObjectProperty(KEY_AUTHOR, "attributes");

        Object phoneDescription = attributes.getProperties().get("phone").getDescription();
        assertNull(phoneDescription);
    }

    @Test
    public void testExample() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");

        Object phoneExample = attributes.getProperties().get("phone").getExample();
        assertEquals("555-000-1111", phoneExample);
    }

    @Test
    public void testExampleRelationship() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "relationships");

        Object authorsExample = attributes.getProperties().get("authors").getExample();
        assertEquals("[\"author1\", \"author2\", \"author3\"]", authorsExample);
    }

    @Test
    public void testNoExampleWithoutAnnotation() {
        ObjectProperty attributes = getObjectProperty(KEY_AUTHOR, "attributes");

        Object nameExample = attributes.getProperties().get("name").getExample();
        assertNull(nameExample);
    }

    @Test
    public void testNoExampleWhenNotProvided() {
        ObjectProperty attributes = getObjectProperty(KEY_AUTHOR, "attributes");

        Object phoneExample = attributes.getProperties().get("phone").getExample();
        assertNull(phoneExample);
    }

    @Test
    public void testConcatenatedDescriptionAndPermissions() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        String yearDescription = attributes.getProperties().get("year").getDescription();
        assertEquals("Year published\nRead Permissions : (Principal is author OR Principal is publisher)",
                yearDescription);
    }

    @Test
    public void testConcatenatedDescriptionAndPermissionsRelationship() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "relationships");

        String authorsDescription = attributes.getProperties().get("authors").getDescription();
        assertEquals("Writers\nRead Permissions : (Principal is author OR Principal is publisher)"
                + "\nUpdate Permissions : (Principal is author)", authorsDescription);
    }

    @Test
    public void testRequired() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        boolean titleRequired = attributes.getProperties().get("title").getRequired();
        assertTrue(titleRequired);
    }

    @Test
    public void testReadOnly() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        Boolean titleReadOnly = attributes.getProperties().get("year").getReadOnly();
        assertTrue(titleReadOnly);
    }

    @Test
    public void testRequiredRelationship() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "relationships");

        Boolean authorsRequired = attributes.getProperties().get("authors").getRequired();
        assertFalse(authorsRequired);
    }

    @Test
    public void testReadOnlyRelationship() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "relationships");

        Boolean authorsReadOnly = attributes.getProperties().get("authors").getReadOnly();
        assertTrue(authorsReadOnly);
    }

    private Resource getModel(String entityKey) {
        Map<String, Model> models = converters.readAll(ENTITIES.get(entityKey));
        return (Resource) models.get(entityKey);
    }

    private ObjectProperty getObjectProperty(String entityKey, String propertyKey) {
        return (ObjectProperty) getModel(entityKey).getProperties().get(propertyKey);
    }

    private StringProperty getStringProperty(String entityKey, String propertyKey) {
        return (StringProperty) getModel(entityKey).getProperties().get(propertyKey);
    }
}
