/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestAuditLogger extends AuditLogger {
    @Override
    public void commit() throws IOException {
    }

    public List<LogMessage> getMessages() {
        return new ArrayList<>(this.messages.get());
    }
}
