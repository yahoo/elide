/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * This interface allows easy overriding of the default OpenApiCustomizer
 * provided by Elide for SpringDoc.
 */
public interface ElideOpenApiCustomizer extends OpenApiCustomizer {
}
