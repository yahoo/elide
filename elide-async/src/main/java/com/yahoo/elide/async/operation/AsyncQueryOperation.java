/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;

/**
 * AsyncQuery Execute Operation Interface.
 */
public interface AsyncQueryOperation extends AsyncAPIOperation<AsyncQuery> {

    /**
     * Calculate Record Count in the response.
     * @param queryObj AsyncAPI type object.
     * @param response ElideResponse object.
     * @return Integer record count
     */
    public abstract Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response);

}
