/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.standalone.interfaces;

import com.paiondata.elide.core.security.checks.Check;

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
