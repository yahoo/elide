/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * test bean.
 */
@Entity
@Include(rootLevel = false)
@Data
public class FirstBean {
    @Id
    @GeneratedValue
    private String id;
    private String name;
    private ComplexAttribute complex;
}
