/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

/**
 * Used to customize the mutable {@link JsonApiSettingsBuilder}.
 */
public interface JsonApiSettingsBuilderCustomizer {
    public void customize(JsonApiSettingsBuilder builder);
}
