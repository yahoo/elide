/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.model.Resource;
import com.yahoo.elide.contrib.swagger.models.Author;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.collect.Maps;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.properties.ObjectProperty;

import java.util.Map;


public class JsonApiModelResolverTest {
    EntityDictionary dictionary;

    @BeforeSuite
    public void setup() {
        dictionary = new EntityDictionary(Maps.newHashMap());

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
    }

    @Test
    public void testBookPermissions() throws Exception {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Book.class);

        Resource bookModel = (Resource) models.get("book");

        String entityPermissions = bookModel.getProperties().get("type").getDescription();
        Assert.assertEquals(entityPermissions,
                "Create Permissions : (Principal is author)\nDelete Permissions : (Deny All)");


        ObjectProperty attributes = (ObjectProperty) bookModel.getProperties().get("attributes");
        ObjectProperty relationships = (ObjectProperty) bookModel.getProperties().get("relationships");

        String titlePermissions = attributes.getProperties().get("title").getDescription();
        Assert.assertEquals(titlePermissions,  "Read Permissions : (Principal is author OR Principal is publisher)");

        String publisherPermissions = relationships.getProperties().get("publisher").getDescription();
        Assert.assertEquals(publisherPermissions,
                "Read Permissions : (Principal is author OR Principal is publisher)\n"
                        + "Update Permissions : (Principal is publisher)");
    }

    @Test
    public void testModelResolution() throws Exception {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Publisher.class);

        Resource publisherModel = (Resource) models.get("publisher");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");
        ObjectProperty relationships = (ObjectProperty) publisherModel.getProperties().get("relationships");

        Assert.assertEquals(attributes.getProperties().size(), 3);
        Assert.assertEquals(relationships.getProperties().size(), 2);
        Assert.assertTrue(attributes.getProperties().containsKey("billingAddress"));
        Assert.assertTrue(attributes.getProperties().containsKey("billingCodes"));
        Assert.assertTrue(relationships.getProperties().containsKey("books"));
        Assert.assertTrue(relationships.getProperties().containsKey("exclusiveAuthors"));
    }

    @Test
    public void testDescription() {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Publisher.class);

        Resource publisherModel = (Resource) models.get("publisher");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");

        String phoneDescription = attributes.getProperties().get("phone").getDescription();
        Assert.assertEquals(phoneDescription,  "Phone number");
    }

    @Test
    public void testExample() {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Publisher.class);

        Resource publisherModel = (Resource) models.get("publisher");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");

        Object phoneExample = attributes.getProperties().get("phone").getExample();
        Assert.assertEquals(phoneExample,  "555-000-1111");
    }

    @Test
    public void testConcatenatedDescriptionAndPermissions() {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Book.class);

        Resource bookModel = (Resource) models.get("book");

        ObjectProperty attributes = (ObjectProperty) bookModel.getProperties().get("attributes");

        String isbnDescription = attributes.getProperties().get("year").getDescription();
        Assert.assertEquals(isbnDescription,  "Year published\nRead Permissions : (Principal is author OR Principal is publisher)");
    }

    @Test
    public void testRequired() {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Book.class);

        Resource publisherModel = (Resource) models.get("book");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");

        boolean titleRequired = attributes.getProperties().get("title").getRequired();
        Assert.assertTrue(titleRequired);
    }

    @Test
    public void testReadOnly() {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Book.class);

        Resource publisherModel = (Resource) models.get("book");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");

        Boolean titleReadOnly = attributes.getProperties().get("year").getReadOnly();
        Assert.assertTrue(titleReadOnly);
    }
}
