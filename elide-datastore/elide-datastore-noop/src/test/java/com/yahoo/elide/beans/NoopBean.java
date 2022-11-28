/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.beans;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Id;

/**
 * Simple bean intended to not be persisted.
 */
@Include(rootLevel = false, name = "theNoopBean")
public class NoopBean {
    private Long id;
    private String test;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
