/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.utils.coerce.converters.TimeZoneSerde;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;
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
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ApiDocsResourceConfig extends ResourceConfig {

    public ApiDocsResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                EntityDictionary dictionary = EntityDictionary.builder().serdeLookup(clazz -> {
                    if (TimeZone.class.equals(clazz)) {
                        return new TimeZoneSerde();
                    }
                    return null;
                }).build();

                dictionary.bindEntity(Book.class);
                dictionary.bindEntity(BookV2.class);
                dictionary.bindEntity(Author.class);
                dictionary.bindEntity(Publisher.class);

                bindFactory(new Factory<List<ApiDocsEndpoint.ApiDocsRegistration>>() {

                    @Override
                    public List<ApiDocsEndpoint.ApiDocsRegistration> provide() {
                        Info info1 = new Info().title("Test Service");

                        OpenApiBuilder builder1 = new OpenApiBuilder(dictionary).apiVersion(info1.getVersion())
                                .supportLegacyFilterDialect(false);
                        OpenAPI openApi1 = builder1.build().info(info1);

                        Info info2 = new Info().title("Test Service").version("1.0");
                        OpenApiBuilder builder2 = new OpenApiBuilder(dictionary).apiVersion(info2.getVersion());
                        OpenAPI openApi2 = builder2.build().info(info2);

                        List<ApiDocsEndpoint.ApiDocsRegistration> docs = new ArrayList<>();
                        docs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", () -> openApi1, "3.0", info1.getVersion()));
                        docs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", () -> openApi2, "3.0", info2.getVersion()));
                        return docs;
                    }

                    @Override
                    public void dispose(List<ApiDocsEndpoint.ApiDocsRegistration> instance) {
                        //NOP
                    }
                }).to(new TypeLiteral<List<ApiDocsEndpoint.ApiDocsRegistration>>() {
                }).named("apiDocs");

                JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder().joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                        .joinFilterDialect(new DefaultFilterDialect(dictionary))
                        .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                        .subqueryFilterDialect(new DefaultFilterDialect(dictionary));

                Elide elide = new Elide(ElideSettings.builder().dataStore(
                        new HashMapDataStore(Arrays.asList(Book.class, BookV2.class, Author.class, Publisher.class)))
                        .auditLogger(new Slf4jLogger())
                        .entityDictionary(dictionary)
                        .verboseErrors(true)
                        .maxPageSize(Pagination.MAX_PAGE_SIZE)
                        .defaultPageSize(Pagination.DEFAULT_PAGE_SIZE)
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")))
                        .settings(jsonApiSettings)
                        .build());

                bind(elide).to(Elide.class).named("elide");
            }
        });
    }
}
