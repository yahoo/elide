/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.google.common.collect.ImmutableMap;
import com.yahoo.elide.contrib.swagger.model.Resource;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.collect.Maps;

import io.swagger.models.properties.StringProperty;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.properties.ObjectProperty;

import java.util.Map;


public class JsonApiModelResolverTest {

    private static final String KEY_BOOK = "book";
    private static final String KEY_PUBLISHER = "publisher";

    private static final Map<String, Class> ENTITIES =
        ImmutableMap.of(KEY_BOOK, Book.class,
                        KEY_PUBLISHER, Publisher.class);

    private ModelConverters converters;

    @BeforeSuite
    public void setup() {
        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());

        dictionary.bindEntity(ENTITIES.get(KEY_BOOK));
        dictionary.bindEntity(ENTITIES.get(KEY_PUBLISHER));

        converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));
    }

    @Test
    public void testBookPermissions() {
        StringProperty entity = getStringProperty(KEY_BOOK, "type");
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");
        ObjectProperty relationships = getObjectProperty(KEY_BOOK, "relationships");

        String entityPermissions = entity.getDescription();
        Assert.assertEquals(entityPermissions,
            "Create Permissions : (Principal is author)\nDelete Permissions : (Deny All)");

        String titlePermissions = attributes.getProperties().get("title").getDescription();
        Assert.assertEquals(titlePermissions,  "Read Permissions : (Principal is author OR Principal is publisher)");

        String publisherPermissions = relationships.getProperties().get("publisher").getDescription();
        Assert.assertEquals(publisherPermissions,
                "Read Permissions : (Principal is author OR Principal is publisher)\n"
                        + "Update Permissions : (Principal is publisher)");
    }

    @Test
    public void testModelResolution() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");
        ObjectProperty relationships = getObjectProperty(KEY_PUBLISHER, "relationships");

        Assert.assertEquals(attributes.getProperties().size(), 3);
        Assert.assertEquals(relationships.getProperties().size(), 2);
        Assert.assertTrue(attributes.getProperties().containsKey("billingAddress"));
        Assert.assertTrue(attributes.getProperties().containsKey("billingCodes"));
        Assert.assertTrue(relationships.getProperties().containsKey("books"));
        Assert.assertTrue(relationships.getProperties().containsKey("exclusiveAuthors"));
    }

    @Test
    public void testDescription() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");

        String phoneDescription = attributes.getProperties().get("phone").getDescription();
        Assert.assertEquals(phoneDescription,  "Phone number");
    }

    @Test
    public void testExample() {
        ObjectProperty attributes = getObjectProperty(KEY_PUBLISHER, "attributes");

        Object phoneExample = attributes.getProperties().get("phone").getExample();
        Assert.assertEquals(phoneExample,  "555-000-1111");
    }

    @Test
    public void testConcatenatedDescriptionAndPermissions() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        String yearDescription = attributes.getProperties().get("year").getDescription();
        Assert.assertEquals(yearDescription,
            "Year published\nRead Permissions : (Principal is author OR Principal is publisher)");
    }

    @Test
    public void testRequired() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        boolean titleRequired = attributes.getProperties().get("title").getRequired();
        Assert.assertTrue(titleRequired);
    }

    @Test
    public void testReadOnly() {
        ObjectProperty attributes = getObjectProperty(KEY_BOOK, "attributes");

        Boolean titleReadOnly = attributes.getProperties().get("year").getReadOnly();
        Assert.assertTrue(titleReadOnly);
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
