/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.executors;

import com.yahoo.elide.core.RequestScope;

/**
 * Permission executor that returns verbose debug to the client.
 */
public class VerbosePermissionExecutor extends ActivePermissionExecutor {

    /**
     * Constructor.
     *
     * @param requestScope Request scope.
     */
    public VerbosePermissionExecutor(RequestScope requestScope) {
        super(requestScope);
    }

    @Override
    public boolean isVerbose() {
        return true;
    }
}
