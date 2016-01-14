/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

/**
 * Commit check interface.
 * @see com.yahoo.elide.security.Check
 *
 * Commit checks are run immediately before a transaction is about to commit but after all changes have been made.
 * Objects passed to this check are guaranteed to be in their final state.
 *
 * @param <T> Type parameter
 */
public interface CommitCheck<T> extends Check<T> {
}
