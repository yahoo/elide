/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base Audit Logger
 * <p>
 * This class uses ThreadLocal list to be thread safe.
 */
public abstract class AuditLogger {
    protected static final ThreadLocal<List<LogMessage>> MESSAGES =
        ThreadLocal.withInitial(ArrayList::new);

    public void log(LogMessage message) {
        MESSAGES.get().add(message);
    }

    public abstract void commit() throws IOException;

    public void clear() {
        List<LogMessage> remainingMessages = MESSAGES.get();
        if (remainingMessages != null) {
            remainingMessages.clear();
        }
    }
}
