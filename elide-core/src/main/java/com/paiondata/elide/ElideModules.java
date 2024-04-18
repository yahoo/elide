/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

/**
 * Used to dynamically determine if a module is available.
 */
public class ElideModules {
    private ElideModules() {
    }

    private static final boolean GRAPHQL_PRESENT;
    private static final boolean JSON_API_PRESENT;
    private static final boolean ASYNC_PRESENT;

    static {
        GRAPHQL_PRESENT = isPresent(
                "com.paiondata.elide.graphql.GraphQLSettings",
                ElideModules.class.getClassLoader());
        JSON_API_PRESENT = isPresent(
                "com.paiondata.elide.jsonapi.JsonApiSettings",
                ElideModules.class.getClassLoader());
        ASYNC_PRESENT = isPresent(
                "com.paiondata.elide.async.AsyncSettings",
                ElideModules.class.getClassLoader());
    }

    /**
     * Checks if com.paiondata.elide.graphql is present.
     *
     * @return true if the GraphQL module is present
     */
    public static boolean isGraphQLPresent() {
        return GRAPHQL_PRESENT;
    }

    /**
     * Checks if com.paiondata.elide.jsonapi is present.
     *
     * @return true if the JsonApi module is present
     */
    public static boolean isJsonApiPresent() {
        return JSON_API_PRESENT;
    }

    /**
     * Checks if com.paiondata.elide.async is present.
     *
     * @return true if the Async module is present
     */
    public static boolean isAsyncPresent() {
        return ASYNC_PRESENT;
    }

    private static boolean isPresent(String name, ClassLoader classLoader) {
        try {
            Class.forName(name, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
