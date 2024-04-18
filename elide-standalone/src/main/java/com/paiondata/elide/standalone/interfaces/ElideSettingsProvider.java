/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.standalone.interfaces;

import com.paiondata.elide.ElideSettings;

/**
 * Standalone ElideSettingsProvider.
 */
public interface ElideSettingsProvider {

    /**
     * Provider for Elide settings.
     *
     * @return Elide settings object.
     */
    ElideSettings getElideSettings();
}
