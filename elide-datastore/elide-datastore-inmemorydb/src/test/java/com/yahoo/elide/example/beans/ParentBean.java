/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * test bean for inheritance.
 */
@Entity
@Include(rootLevel = false)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ParentBean {
    public String id;

    @Id
    @GeneratedValue
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
