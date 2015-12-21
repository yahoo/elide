/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.security.User;

/**
 * Commit check interface.
 * @see Check
 *
 * Commit checks are run immediately before a transaction is about to commit but after all changes have been made.
 * Objects passed to this check are guaranteed to be in their final state.
 *
 * @param <T> Type parameter
 */
public abstract class CommitCheck<T> implements Check<T> {
    /* NOTE: Operation checks and user checks are intended to be _distinct_ */
    @Override
    public final boolean ok(User user) {
        throw new UnsupportedOperationException();
    }
}
