/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;


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
     public static Map<String, List<String>> removeAuthHeaders(Map<String, List<String>> headers) {
         Map<String, List<String>> requestHeaders = new HashMap<>(headers);
         if (requestHeaders.get(HttpHeaders.AUTHORIZATION) != null) {
             requestHeaders.remove(HttpHeaders.AUTHORIZATION);
         }
         if (requestHeaders.get("Proxy-Authorization") != null) {
             requestHeaders.remove("Proxy-Authorization");
         }
         return requestHeaders;
     }
}
