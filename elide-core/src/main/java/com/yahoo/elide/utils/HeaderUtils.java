/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utility class which modifies request headers
 */
public class HeaderUtils {

    /**
     * Resolve value of api version from request headers
     * @param headers HttpHeaders
     * @return apiVersion
     */
     public static String resolveApiVersion(HttpHeaders headers) {
         List<String> apiVersionList = headers.getRequestHeader("ApiVersion");
         String apiVersion = NO_VERSION;
         if (apiVersionList != null && apiVersionList.size() == 1) {
             apiVersion = apiVersionList.get(0);
         }
         return apiVersion;
     }

     /**
      * Remove Authorization and Proxy Authorization headers from request headers
      * @param headers HttpHeaders
      * @return requestHeaders
      */
     public static MultivaluedMap<String, String> removeAuthHeaders(HttpHeaders headers) {
         MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
         if (requestHeaders != null && headers.getRequestHeader(HttpHeaders.AUTHORIZATION) != null) {
             requestHeaders.remove(HttpHeaders.AUTHORIZATION);
         }
         if (requestHeaders != null && headers.getRequestHeader("Proxy-Authorization") != null) {
             requestHeaders.remove("Proxy-Authorization");
         }
         return requestHeaders;
     }
}
