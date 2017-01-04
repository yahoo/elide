/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.core.RequestScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base Audit Logger
 * <p>
 * This class uses ThreadLocal list to be thread safe.
 */
public abstract class AuditLogger {
    protected final ThreadLocal<List<LogMessage>> messages;

    public AuditLogger() {
        messages = ThreadLocal.withInitial(ArrayList::new);
    }

    public void log(LogMessage message) {
        messages.get().add(message);
    }

    public abstract void commit(RequestScope requestScope) throws IOException;

    public void clear() {
        List<LogMessage> remainingMessages = messages.get();
        if (remainingMessages != null) {
            remainingMessages.clear();
        }
    }
}
