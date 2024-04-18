/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;

/**
 * Utility methods for customizing a {@link GraphQLSettingsBuilder}.
 */
public class GraphQLSettingsBuilderCustomizers {
    private GraphQLSettingsBuilderCustomizers() {
    }

    public static GraphQLSettingsBuilder buildGraphQLSettingsBuilder(EntityDictionary entityDictionary,
            GraphQLSettingsBuilderCustomizer customizer) {
        GraphQLSettingsBuilder builder = GraphQLSettingsBuilder.withDefaults(entityDictionary);
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
