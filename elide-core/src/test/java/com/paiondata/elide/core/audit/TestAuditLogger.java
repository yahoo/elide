/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestAuditLogger extends AuditLogger {
    public TestAuditLogger() {
        // clean any prior test data for this thread
        super.clear();
    }

    @Override
    public void commit() throws IOException {
        //NOOP
    }

    public List<LogMessage> getMessages() {
        return new ArrayList<>(this.MESSAGES.get());
    }
}
