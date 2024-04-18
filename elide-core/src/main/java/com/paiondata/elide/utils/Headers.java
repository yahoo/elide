/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility class which modifies request headers.
 */
public class Headers {
    private Headers() {
    }

    private static final Set<String> AUTHORIZATION_HEADER_NAMES;

    static {
        AUTHORIZATION_HEADER_NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        AUTHORIZATION_HEADER_NAMES.addAll(Set.of("authorization", "proxy-authorization"));
    }

    /**
     * Remove Authorization and Proxy Authorization headers from request headers.
     * @param headers the headers to process
     * @return the processed headers or original headers
     */
     public static Map<String, List<String>> removeAuthorizationHeaders(Map<String, List<String>> headers) {
         boolean contains = false;
         for (String headerName : headers.keySet()) {
             if (AUTHORIZATION_HEADER_NAMES.contains(headerName)) {
                 contains = true;
                 break;
             }
         }
         if (!contains) {
             return headers;
         }

         // HTTP header names should be case insensitive, but maybe not all libraries consider this
         Map<String, List<String>> requestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

         headers.entrySet().stream().forEach(entry -> {
             if (!AUTHORIZATION_HEADER_NAMES.contains(entry.getKey())) {
                 requestHeaders.put(entry.getKey(), entry.getValue());
             }
         });
         return requestHeaders;
     }
}
