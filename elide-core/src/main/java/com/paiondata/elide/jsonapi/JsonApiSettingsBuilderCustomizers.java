/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

/**
 * Utility methods for customizing a {@link JsonApiSettingsBuilder}.
 */
public class JsonApiSettingsBuilderCustomizers {
    private JsonApiSettingsBuilderCustomizers() {
    }

    public static JsonApiSettingsBuilder buildJsonApiSettingsBuilder(EntityDictionary entityDictionary,
            JsonApiSettingsBuilderCustomizer customizer) {
        JsonApiSettingsBuilder builder = JsonApiSettingsBuilder.withDefaults(entityDictionary);
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
