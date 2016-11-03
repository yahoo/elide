/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.beans;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;

/**
 * Simple bean intended to not be persisted
 */
@Entity
@Include(type = "theNoopBean")
public class NoopBean {
    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
