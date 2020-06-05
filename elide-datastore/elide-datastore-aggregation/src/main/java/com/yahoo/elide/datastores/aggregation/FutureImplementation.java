/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryEngine;

import java.util.concurrent.Future;

public abstract class FutureImplementation implements Future<QueryResult> {

    /**
     * Cancels transaction
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        SQLQueryEngine.cancel();
    }
}
