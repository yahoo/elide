/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.ElideSettings.ElideSettingsBuilder;

/**
 * Used to customize the mutable {@link ElideSettingsBuilder}.
 */
public interface ElideSettingsBuilderCustomizer {
    public void customize(ElideSettingsBuilder builder);
}
