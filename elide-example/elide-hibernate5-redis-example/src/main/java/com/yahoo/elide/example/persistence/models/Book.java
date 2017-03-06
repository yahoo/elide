/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.persistence.models;

import com.yahoo.elide.annotation.Include;

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
 * Model for books
 */
@Entity
@Indexed
@Include(rootLevel = true)
public class Book {
    private long id;
    private String title;
    private String genre;
    private String language;
    private Collection<Author> authors = new ArrayList<>();

    public Book() {

    }

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "book")
    @TableGenerator(name = "book")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @ManyToMany
    public Collection<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Collection<Author> authors) {
        this.authors = authors;
    }
}
