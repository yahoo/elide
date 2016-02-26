/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.security.User;

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
public abstract class OperationCheck<T> extends InlineCheck<T> {
    /* NOTE: Operation checks and user checks are intended to be _distinct_ */
    @Override
    public final boolean ok(User user) {
        throw new UnsupportedOperationException();
    }
}
