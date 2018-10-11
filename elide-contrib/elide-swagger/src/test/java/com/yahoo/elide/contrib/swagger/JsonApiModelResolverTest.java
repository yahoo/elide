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
                "Create Permissions : (Principal is author)\nDelete Permissions : (Deny All)\n");


        ObjectProperty attributes = (ObjectProperty) bookModel.getProperties().get("attributes");
        ObjectProperty relationships = (ObjectProperty) bookModel.getProperties().get("relationships");

        String titlePermissions = attributes.getProperties().get("title").getDescription();
        Assert.assertEquals(titlePermissions,  "Read Permissions : (Principal is author OR Principal is publisher)\n");

        String publisherPermissions = relationships.getProperties().get("publisher").getDescription();
        Assert.assertEquals(publisherPermissions,
                "Read Permissions : (Principal is author OR Principal is publisher)\n"
                        + "Update Permissions : (Principal is publisher)\n");
    }

    @Test
    public void testModelResolution() throws Exception {
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        Map<String, Model> models = converters.readAll(Publisher.class);

        Resource publisherModel = (Resource) models.get("publisher");

        ObjectProperty attributes = (ObjectProperty) publisherModel.getProperties().get("attributes");
        ObjectProperty relationships = (ObjectProperty) publisherModel.getProperties().get("relationships");

        Assert.assertEquals(attributes.getProperties().size(), 2);
        Assert.assertEquals(relationships.getProperties().size(), 2);
        Assert.assertTrue(attributes.getProperties().containsKey("billingAddress"));
        Assert.assertTrue(attributes.getProperties().containsKey("billingCodes"));
        Assert.assertTrue(relationships.getProperties().containsKey("books"));
        Assert.assertTrue(relationships.getProperties().containsKey("exclusiveAuthors"));
    }
}
