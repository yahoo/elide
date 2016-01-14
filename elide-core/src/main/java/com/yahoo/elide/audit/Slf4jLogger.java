/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Logger implementation which logs to SLF4J.
 */
@Slf4j
public class Slf4jLogger extends Logger {

    @Override
    public void commit() throws IOException {
        RuntimeException cause = null;
        for (LogMessage message : messages.get()) {
            try {
                log.info("{} {} {}", System.currentTimeMillis(), message.getOperationCode(), message.getMessage());
            } catch (RuntimeException e) {
                if (cause != null) {
                    cause.addSuppressed(e);
                } else {
                    cause = e;
                }
            }
        }
        messages.get().clear();
        if (cause != null) {
            throw cause;
        }
    }
}
