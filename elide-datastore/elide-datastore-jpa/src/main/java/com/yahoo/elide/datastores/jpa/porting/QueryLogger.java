/*
 * Copyright 2021, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.porting;

@FunctionalInterface
public interface QueryLogger {
    void log(String query);
}
