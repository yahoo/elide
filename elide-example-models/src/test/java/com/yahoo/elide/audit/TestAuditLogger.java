/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import com.yahoo.elide.core.RequestScope;

import java.io.IOException;

/**
 * Example Logger test.
 */
public class TestAuditLogger extends AuditLogger {
    public int commitCount = 0;
    public int logCount = 0;

    @Override
    public void log(LogMessage message) {
        super.log(message);
        logCount++;
    }

    @Override
    public void commit(RequestScope requestScope) throws IOException {
        commitCount++;
    }
}
