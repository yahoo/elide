/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import org.springdoc.core.models.GroupedOpenApi;

/**
 * Customize the GroupedOpenApi.
 */
public interface ElideGroupedOpenApiCustomizer {
    void customize(GroupedOpenApi groupedOpenApi);
}
