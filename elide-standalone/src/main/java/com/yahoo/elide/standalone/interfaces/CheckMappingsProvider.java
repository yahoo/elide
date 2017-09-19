/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.interfaces;

import com.yahoo.elide.security.checks.Check;

import java.util.Map;

/**
 * Provider for check mappings.
 */
public interface CheckMappingsProvider {

    /**
     * Get check mappings.
     *
     * @return Check mappings.
     */
    Map<String, Class<? extends Check>> getCheckMappings();
}
