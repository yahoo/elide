/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.core.RequestScope;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Logger implementation which logs to SLF4J.
 */
@Slf4j
public class Slf4jLogger extends AuditLogger {

    @Override
    public void commit(RequestScope requestScope) throws IOException {
        try {
            for (LogMessage message : messages.get()) {
                log.info("{} {} {}", System.currentTimeMillis(), message.getOperationCode(), message.getMessage());
            }
        } finally {
            messages.get().clear();
        }
    }
}
