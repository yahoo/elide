/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

import com.paiondata.elide.async.AsyncSettings.AsyncSettingsBuilder;

/**
 * Utility methods for customizing a {@link AsyncSettingsBuilder}.
 */
public class AsyncSettingsBuilderCustomizers {
    private AsyncSettingsBuilderCustomizers() {
    }

    public static AsyncSettingsBuilder buildAsyncSettingsBuilder(AsyncSettingsBuilderCustomizer customizer) {
        AsyncSettingsBuilder builder = new AsyncSettingsBuilder();
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
