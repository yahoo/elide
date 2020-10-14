/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;

/**
 * child test bean for inheritance.
 */
@Entity
@Include(rootLevel = false)
public class ChildBean extends ParentBean {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
