/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;

import java.util.Map;

public class HistoryContainer extends RootContainer {
    public static final String HISTORY_KEY = "__history";
    private static final String DATE_KEY = "date";
    private static final String REVISION_KEY = "revision";

    /**
     * Constructor.
     *
     * @param context Context containing history field.
     */
    HistoryContainer(Environment context) {
        Map<String, Object> args = context.arguments;
        if (args != null) {
            context.requestScope.setHistoricalDate((String) args.get(DATE_KEY));
            context.requestScope.setHistoricalVersion((String) args.get(REVISION_KEY));
        }
    }
}
