/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async;

import com.yahoo.elide.async.AsyncSettings.AsyncSettingsBuilder;

/**
 * Used to customize the mutable {@link AsyncSettingsBuilder}.
 */
public interface AsyncSettingsBuilderCustomizer {
    public void customize(AsyncSettingsBuilder builder);
}
