/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.Exclude;

/**
 * Exclude Test bean.
 */
@Exclude
public class ExcludedBean {
    private boolean excluded;
}
