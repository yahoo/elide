/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.swagger.resources.ApiDocsEndpoint;
import example.models.Author;
import example.models.Book;
import example.models.Publisher;
import example.models.versioned.BookV2;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import java.util.ArrayList;
import java.util.List;

public class ApiDocsResourceConfig extends ResourceConfig {

    public ApiDocsResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new Factory<List<ApiDocsEndpoint.ApiDocsRegistration>>() {

                    @Override
                    public List<ApiDocsEndpoint.ApiDocsRegistration> provide() {
                        EntityDictionary dictionary = EntityDictionary.builder().build();

                        dictionary.bindEntity(Book.class);
                        dictionary.bindEntity(BookV2.class);
                        dictionary.bindEntity(Author.class);
                        dictionary.bindEntity(Publisher.class);
                        Info info1 = new Info().title("Test Service");

                        OpenApiBuilder builder1 = new OpenApiBuilder(dictionary, info1).withLegacyFilterDialect(false);
                        OpenAPI openApi1 = builder1.build();

                        Info info2 = new Info().title("Test Service").version("1.0");
                        OpenApiBuilder builder2 = new OpenApiBuilder(dictionary, info2);
                        OpenAPI openApi2 = builder2.build();

                        List<ApiDocsEndpoint.ApiDocsRegistration> docs = new ArrayList<>();
                        docs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", openApi1));
                        docs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", openApi2));
                        return docs;
                    }

                    @Override
                    public void dispose(List<ApiDocsEndpoint.ApiDocsRegistration> instance) {
                        //NOP
                    }
                }).to(new TypeLiteral<List<ApiDocsEndpoint.ApiDocsRegistration>>() {
                }).named("apiDocs");
            }
        });
    }
}
