/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

/**
 * Operation check interface.
 * @see com.yahoo.elide.security.Check
 *
 * Operation checks are run in-line (i.e. as soon as objects are first encountered).
 *
 * <b>NOTE:</b> For <em>non-Read</em> operations, the object passed to this interface is not guaranteed to be complete
 *              as it will run _BEFORE_ changes are made to the object.
 *
 * @param <T> Type parameter
 */
public interface OperationCheck<T> extends Check<T> {
}
