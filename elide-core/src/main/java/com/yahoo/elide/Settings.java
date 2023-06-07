/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

/**
 * Used for defining additional settings.
 */
public interface Settings {

    public SettingsBuilder mutate();

    /**
     * Mutable builder for defining additional settings.
     */
    public interface SettingsBuilder {
        Settings build();
    }
}
