/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpql.query;

import com.yahoo.elide.datastores.jpql.porting.QueryLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultQueryLogger implements QueryLogger {
    @Override
    public void log(String query) {
       log.debug("{}", query);
    }
}
