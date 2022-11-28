/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import example.models.Author;
import example.models.Book;
import example.models.Publisher;
import example.models.versioned.BookV2;
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
                        EntityDictionary dictionary = EntityDictionary.builder().build();

                        dictionary.bindEntity(Book.class);
                        dictionary.bindEntity(BookV2.class);
                        dictionary.bindEntity(Author.class);
                        dictionary.bindEntity(Publisher.class);
                        Info info1 = new Info().title("Test Service");

                        SwaggerBuilder builder1 = new SwaggerBuilder(dictionary, info1).withLegacyFilterDialect(false);
                        Swagger swagger1 = builder1.build();

                        Info info2 = new Info().title("Test Service").version("1.0");
                        SwaggerBuilder builder2 = new SwaggerBuilder(dictionary, info2);
                        Swagger swagger2 = builder2.build();

                        List<DocEndpoint.SwaggerRegistration> docs = new ArrayList<>();
                        docs.add(new DocEndpoint.SwaggerRegistration("test", swagger1));
                        docs.add(new DocEndpoint.SwaggerRegistration("test", swagger2));
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
