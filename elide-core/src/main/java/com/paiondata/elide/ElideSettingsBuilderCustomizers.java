/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.ElideSettings.ElideSettingsBuilder;

/**
 * Utility methods for customizing a {@link ElideSettingsBuilder}.
 */
public class ElideSettingsBuilderCustomizers {
    private ElideSettingsBuilderCustomizers() {
    }

    public static ElideSettingsBuilder buildElideSettingsBuilder(ElideSettingsBuilderCustomizer customizer) {
        ElideSettingsBuilder builder = new ElideSettingsBuilder();
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
