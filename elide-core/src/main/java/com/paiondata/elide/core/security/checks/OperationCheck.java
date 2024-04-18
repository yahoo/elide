/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.checks;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

import java.util.Optional;

/**
 * Operation check interface.
 * @see Check
 *
 * Operation checks are run in-line (i.e. as soon as objects are first encountered).
 *
 * <b>NOTE:</b> For <em>non-Read</em> operations, the object passed to this interface is not guaranteed to be complete
 *              as it will run _BEFORE_ changes are made to the object.
 *
 * @param <T> Type parameter
 */
public abstract class OperationCheck<T> implements Check {
    /**
     * Determines whether the user can access the resource.
     *
     * @param object Fully modified object
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if security check passed
     */
    public abstract boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec);
}
