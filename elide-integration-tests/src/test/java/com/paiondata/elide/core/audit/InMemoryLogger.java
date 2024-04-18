/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.audit;

import com.paiondata.elide.core.security.ChangeSpec;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Set;

/**
 * Audit logger that stores messages in memory.
 */
public class InMemoryLogger extends AuditLogger {
    public final Set<String> logMessages = Sets.newConcurrentHashSet();

    @Override
    public void commit() throws IOException {
        for (LogMessage message : MESSAGES.get()) {
            if (message.getChangeSpec().isPresent()) {
                logMessages.add(changeSpecToString(message.getChangeSpec().get()));
            }
            logMessages.add(message.getMessage());
        }
    }

    private String changeSpecToString(final ChangeSpec changeSpec) {
        if (changeSpec == null) {
            return "null";
        }
        String old = (changeSpec.getOriginal() == null) ? "null" : changeSpec.getOriginal().toString();
        String modified = (changeSpec.getModified() == null) ? "null" : changeSpec.getModified().toString();
        return "old: " + old + "\nnew: " + modified;
    }
}
