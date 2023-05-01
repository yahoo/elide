/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

/**
 * Test for DefaultElideOpenApiCustomizer.
 */
class DefaultElideOpenApiCustomizerTest {

    DefaultElideOpenApiCustomizer customizer;

    @BeforeEach
    void setup() {
        EntityDictionary entityDictionary = Mockito.mock(EntityDictionary.class);
        DataStore dataStore = Mockito.mock(DataStore.class);
        ElideSettings settings = new ElideSettingsBuilder(dataStore).withEntityDictionary(entityDictionary).build();
        Elide elide = new Elide(settings);
        RefreshableElide refreshableElide = new RefreshableElide(elide);

        customizer = new DefaultElideOpenApiCustomizer(refreshableElide, EntityDictionary.NO_VERSION);
    }

    @Test
    void shouldNotThrowForEmptyOpenApi() {
        OpenAPI openApi = new OpenAPI();
        assertThatCode(() -> customizer.customise(openApi)).doesNotThrowAnyException();
    }

    @Test
    void shouldRemovePaths() {
        OpenAPI openApi = new OpenAPI();
        openApi.path("/graphql", new PathItem().post(new Operation().addTagsItem("graphql-controller")));
        openApi.path("/json-api", new PathItem().get(new Operation().addTagsItem("json-api-controller")));
        openApi.path("/api-docs", new PathItem().get(new Operation().addTagsItem("api-docs-controller")));
        customizer.customise(openApi);
        assertThat(openApi.getPaths()).isEmpty();
    }
}
