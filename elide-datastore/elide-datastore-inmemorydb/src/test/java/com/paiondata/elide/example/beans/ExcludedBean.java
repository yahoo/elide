/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.example.beans;

import com.paiondata.elide.annotation.Exclude;

/**
 * Exclude Test bean.
 */
@Exclude
public class ExcludedBean {
    private boolean excluded;
}
