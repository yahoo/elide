/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.models;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Model for books.  Should not break GraphQL schema for example.Book.
 */
@Entity
@Table(name = "book")
@Include(name = "internalBook", description = "A GraphQL Book")

public class Book {
    @Id
    private long id;
    private String title;
}
