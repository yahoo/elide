/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

/**
 * Used for defining additional settings.
 */
public interface Settings {

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public SettingsBuilder mutate();

    /**
     * Mutable builder for defining additional settings.
     */
    public interface SettingsBuilder {

        /**
         * Build the settings.
         *
         * @return the settings
         */
        Settings build();
    }
}
