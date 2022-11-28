/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * test bean.
 */
@Entity
@Include(rootLevel = false)
public class SecondBean {
    @Id
    public int id;

    public int age;
}
