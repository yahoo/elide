/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.audit;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Logger implementation which logs to SLF4J.
 */
@Slf4j
public class Slf4jLogger extends AuditLogger {

    @Override
    public void commit() throws IOException {
        try {
            for (LogMessage message : MESSAGES.get()) {
                log.info("{} {} {}", System.currentTimeMillis(), message.getOperationCode(), message.getMessage());
            }
        } finally {
            MESSAGES.get().clear();
        }
    }
}
