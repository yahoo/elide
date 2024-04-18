/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.test.models;

import com.paiondata.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Include
@Entity
public class Book {
    @Id
    private long id;

    private String title;
}
