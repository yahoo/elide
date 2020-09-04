/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Extra properties for setting up async download support.
 */
@Data
public class DownloadControllerProperties extends ControllerProperties {

    /**
     *  Maximum Download Run time before TIMEOUT.
     */
    private int maxDownloadRunTimeSeconds = 60;

}
