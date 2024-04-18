/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.modelconfig.validator;

import com.paiondata.elide.modelconfig.store.models.ConfigFile;

import java.util.Map;

/**
 * Used to validate configuration.
 */
@FunctionalInterface
public interface Validator {

    /**
     * Validate a full set of configurations.  Throws an exception if there is an error.
     * @param resourceMap Maps the path to the resource content.
     */
    void validate(Map<String, ConfigFile> resourceMap);
}
