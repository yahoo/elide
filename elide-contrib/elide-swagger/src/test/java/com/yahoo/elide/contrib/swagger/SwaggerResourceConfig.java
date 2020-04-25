/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.models.Author;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.contrib.swagger.resources.DocEndpoint;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.collect.Maps;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.util.ArrayList;
import java.util.List;

public class SwaggerResourceConfig extends ResourceConfig {

    public SwaggerResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new Factory<List<DocEndpoint.SwaggerRegistration>>() {

                    @Override
                    public List<DocEndpoint.SwaggerRegistration> provide() {
                        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());

                        dictionary.bindEntity(Book.class);
                        dictionary.bindEntity(Author.class);
                        dictionary.bindEntity(Publisher.class);
                        Info info = new Info().title("Test Service");

                        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
                        Swagger swagger = builder.build();

                        List<DocEndpoint.SwaggerRegistration> docs = new ArrayList<>();
                        docs.add(new DocEndpoint.SwaggerRegistration("test", swagger));
                        return docs;
                    }

                    @Override
                    public void dispose(List<DocEndpoint.SwaggerRegistration> instance) {
                        //NOP
                    }
                }).to(new TypeLiteral<List<DocEndpoint.SwaggerRegistration>>() {
                }).named("swagger");
            }
        });
    }
}
