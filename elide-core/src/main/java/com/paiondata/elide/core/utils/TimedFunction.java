/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Wraps a function and logs how long it took to run (in millis).
 * @param <R> The function return type.
 */
@Slf4j
@Data
public class TimedFunction<R> implements Supplier<R> {

    public TimedFunction(Supplier<R> toRun, String logMessage) {
        this.toRun = toRun;
        this.logMessage = logMessage;
    }

    private Supplier<R> toRun;
    private String logMessage;

    @Override
    public R get() {
        long start = System.currentTimeMillis();
        R ret = toRun.get();
        long end = System.currentTimeMillis();

        log.debug(logMessage + "\tTime spent: {}", end - start);

        return ret;
    }
}
