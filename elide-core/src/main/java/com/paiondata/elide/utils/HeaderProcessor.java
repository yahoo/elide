/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.utils;

import java.util.List;
import java.util.Map;

/**
 * Processes HTTP headers.
 */
@FunctionalInterface
public interface HeaderProcessor {
    /**
     * Processes headers.
     *
     * @param headers the input headers
     * @return the processed headers
     */
    Map<String, List<String>> process(Map<String, List<String>> headers);
}
