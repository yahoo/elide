/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

/**
 * Intermediate check representing the hierarchical structure of checks.
 * For instance, Read/Delete permissions can take any type of InlineCheck
 * while Create/Update permissions can be of any Check type.
 *
 * @param <T> type parameter
 */
public abstract class InlineCheck<T> implements Check<T> {
}
