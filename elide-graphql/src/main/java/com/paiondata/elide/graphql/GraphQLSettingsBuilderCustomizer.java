/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;

/**
 * Used to customize the mutable {@link GraphQLSettingsBuilder}.
 */
public interface GraphQLSettingsBuilderCustomizer {
    public void customize(GraphQLSettingsBuilder builder);
}
