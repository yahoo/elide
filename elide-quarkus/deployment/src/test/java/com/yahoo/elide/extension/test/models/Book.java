/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.test.models;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.OffsetDateTime;

@Include
@Entity
public class Book {
    @Id
    private long id;

    private String title;

    private OffsetDateTime releaseDate;
}
