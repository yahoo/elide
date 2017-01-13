/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.google.common.collect.Maps;
import com.yahoo.elide.contrib.swagger.models.Author;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.core.EntityDictionary;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

public class SwaggerResourceConfig extends ResourceConfig {

    public SwaggerResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new Factory<Swagger>() {

                    @Override
                    public Swagger provide() {
                        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());

                        dictionary.bindEntity(Book.class);
                        dictionary.bindEntity(Author.class);
                        dictionary.bindEntity(Publisher.class);
                        Info info = new Info().title("Test Service").version("1.0");

                        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
                        Swagger swagger = builder.build();

                        return swagger;
                    }

                    @Override
                    public void dispose(Swagger instance) {
                        //NOP
                    }
                }).to(Swagger.class).named("swagger");
            }
        });
    }
}
