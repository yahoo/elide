/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base Audit Logger
 * <p>
 * This class uses synchronized list to be thread safe.
 */
public abstract class Logger {
    protected final List<LogMessage> messages;

    public Logger() {
        messages = Collections.synchronizedList(new ArrayList<>());
    }

    public void log(LogMessage message) {
        messages.add(message);
    }

    public abstract void commit() throws IOException;
}
