/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Model for publisher.
 */
@Entity
@Include
public class Publisher {
    private long id;
    private String name;
    private Set<Book> books = new HashSet<>();

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    @OneToMany(mappedBy = "publisher")
    public Set<Book> getBooks() {
        return books;
    }

    public void setBooks(Set<Book> authors) {
        this.books = books;
    }
}
