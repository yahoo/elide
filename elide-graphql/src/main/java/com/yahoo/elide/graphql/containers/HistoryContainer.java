/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;

import java.util.List;
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
            if (args.get(DATE_KEY) != null && ((List)args.get(DATE_KEY)).size() == 1 ) {
                context.requestScope.setHistoricalDatestamp((Long) ((List) args.get(DATE_KEY)).get(0));
            }
            if (args.get(REVISION_KEY) != null && ((List)args.get(REVISION_KEY)).size() == 1 ) {
                context.requestScope.setHistoricalRevision((Long) ((List) args.get(REVISION_KEY)).get(0));
            }
        }
    }
}
