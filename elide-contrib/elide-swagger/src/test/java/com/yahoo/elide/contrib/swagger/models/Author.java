/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.models;

import com.yahoo.elide.annotation.Include;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
@Include
public class Author {
    @OneToMany
    public Set<Book> getBooks() {
        return null;
    }

    @OneToMany
    public Set<Publisher> getPublisher() {
        return null;
    }

    public AuthorType membershipType;

    public String name;
}
