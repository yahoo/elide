/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.persistence.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.checks.prefab.Role;

import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.TableGenerator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Model for authors
 */
@Entity
@Indexed
@SharePermission(any = {Role.ALL.class})
@Include(rootLevel = true)
public class Author {
    private long id;
    private String name;
    private Collection<Book> books = new ArrayList<>();

    public Author() {

    }

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "author")
    @TableGenerator(name = "author")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToMany(mappedBy = "authors")
    public Collection<Book> getBooks() {
        return books;
    }

    public void setBooks(Collection<Book> books) {
        this.books = books;
    }
}
