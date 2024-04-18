/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.spring.config.ElideConfigProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Test for DefaultElideGroupedOpenApiCustomizer.
 */
class DefaultElideGroupedOpenApiCustomizerTest {

    DefaultElideGroupedOpenApiCustomizer customizer;

    @BeforeEach
    void setup() {
        EntityDictionary entityDictionary = Mockito.mock(EntityDictionary.class);
        DataStore dataStore = Mockito.mock(DataStore.class);
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder().path("/");
        ElideSettings settings = ElideSettings.builder().dataStore(dataStore).entityDictionary(entityDictionary)
                .settings(jsonApiSettings).build();
        when(entityDictionary.getApiVersions()).thenReturn(new LinkedHashSet<>(Arrays.asList(EntityDictionary.NO_VERSION, "1", "2")));
        Elide elide = new Elide(settings);
        RefreshableElide refreshableElide = new RefreshableElide(elide);
        ElideConfigProperties properties = new ElideConfigProperties();

        customizer = new DefaultElideGroupedOpenApiCustomizer(refreshableElide, properties);
    }

    @Test
    void shouldNotThrowForEmptyOpenApi() {
        GroupedOpenApi groupedOpenApi = GroupedOpenApi.builder().group("default").pathsToMatch("/**").build();
        customizer.customize(groupedOpenApi);
        OpenAPI openApi = new OpenAPI();
        assertThatCode(
                () -> groupedOpenApi.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRemovePaths() {
        GroupedOpenApi groupedOpenApi = GroupedOpenApi.builder().group("default").pathsToMatch("/**").build();
        customizer.customize(groupedOpenApi);

        OpenAPI openApi = new OpenAPI();
        openApi.path("/graphql", new PathItem().post(new Operation().addTagsItem("graphql-controller")));
        openApi.path("/json-api", new PathItem().get(new Operation().addTagsItem("json-api-controller")));
        openApi.path("/api-docs", new PathItem().get(new Operation().addTagsItem("api-docs-controller")));
        groupedOpenApi.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));
        assertThat(openApi.getPaths()).isEmpty();
    }
}
