/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.audit;

/**
 * Broker to retrieve logger.
 */
public class LoggerSingleton {
    private static Logger logger = new Slf4jLogger();

    /**
     * Set a logger.
     *
     * @param logger Logger to set
     */
    public static void setLogger(Logger logger) {
        LoggerSingleton.logger = logger;
    }

    /**
     * Get logger.
     *
     * @return Current logger or default if not set
     */
    public static Logger getLogger() {
        return logger;
    }
}
