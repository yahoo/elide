/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class which modifies request headers.
 */
public class HeaderUtils {

    @FunctionalInterface
    public interface HeaderProcessor {
        Map<String, List<String>> process(Map<String, List<String>> headers);
    }

    /**
     * Remove Authorization and Proxy Authorization headers from request headers.
     * @param headers HttpHeaders
     * @return requestHeaders
     */
     public static Map<String, List<String>> lowercaseAndRemoveAuthHeaders(Map<String, List<String>> headers) {
         // HTTP header names should be case insensitive, but maybe not all libraries consider this
         Map<String, List<String>> requestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

         headers.entrySet().stream().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));

         if (requestHeaders.get("authorization") != null) {
             requestHeaders.remove("authorization");
         }
         if (requestHeaders.get("proxy-authorization") != null) {
             requestHeaders.remove("proxy-authorization");
         }
         return requestHeaders;
     }
}
