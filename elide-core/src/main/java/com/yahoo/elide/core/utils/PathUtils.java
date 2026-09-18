/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Utilities for working with request paths.
 */
public class PathUtils {

    private static final Pattern DUPLICATE_SEPARATOR_PATTERN = Pattern.compile("//+");

    private PathUtils() {
        // Utility class
    }

    /**
     * Normalize request path.
     *
     * @param path request path
     * @return normalized path string
     */
    public static String normalizePath(String path) {
        String normalizedPath = DUPLICATE_SEPARATOR_PATTERN.matcher(path).replaceAll("/");

        normalizedPath = StringUtils.removeEnd(normalizedPath, "/");

        return StringUtils.removeStart(normalizedPath, "/");
    }
}
