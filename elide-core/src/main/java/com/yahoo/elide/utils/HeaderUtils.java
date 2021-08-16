/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Utility class which modifies request headers
 */
public class HeaderUtils {

    /**
     * Resolve value of api version from request headers.
     * @param headers HttpHeaders
     * @return apiVersion
     */

     public static String resolveApiVersion(Map<String, List<String>> headers) {
         String apiVersion = NO_VERSION;
         if (headers != null && headers.get("ApiVersion") != null) {
             apiVersion = headers.get("ApiVersion").get(0);
         }
         return apiVersion;
     }
    /**
     * Remove Authorization and Proxy Authorization headers from request headers.
     * @param headers HttpHeaders
     * @return requestHeaders
     */
     public static Map<String, List<String>> lowercaseAndRemoveAuthHeaders(Map<String, List<String>> headers) {
         // HTTP headers should be treated lowercase, but maybe not all libraries consider this
         Map<String, List<String>> requestHeaders = headers.entrySet().stream()
                 .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(Locale.ENGLISH), Map.Entry::getValue));

         if (requestHeaders.get("authorization") != null) {
             requestHeaders.remove("authorization");
         }
         if (requestHeaders.get("proxy-authorization") != null) {
             requestHeaders.remove("proxy-authorization");
         }
         return requestHeaders;
     }
}
