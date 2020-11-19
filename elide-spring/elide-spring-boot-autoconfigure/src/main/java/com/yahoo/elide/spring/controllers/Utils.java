/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.spring.controllers;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

public class Utils {
    public static String getApiVersion(Map<String, String> requestHeaders) {
        return requestHeaders.getOrDefault("ApiVersion",
                requestHeaders.getOrDefault("apiversion", NO_VERSION));  //For tomcat
    }

    public static Map<String, String> removeAuthHeaders(Map<String, String> requestHeaders) {
        Map<String, String> requestHeadersCleaned = new HashMap<String, String>(requestHeaders);
        if (requestHeadersCleaned.get(HttpHeaders.AUTHORIZATION) != null) {
            requestHeadersCleaned.remove(HttpHeaders.AUTHORIZATION);
        }
        if (requestHeadersCleaned.get("Proxy-Authorization") != null) {
            requestHeadersCleaned.remove("Proxy-Authorization");
        }
        return requestHeadersCleaned;
    }
}
