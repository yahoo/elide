/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.books;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.RequestScope;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Model for books.
 */
@Entity
@SharePermission
@Table(name = "book")
@Include(rootLevel = true)
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${book.title}"})
public class Book {
    private long id;
    private String title;
    private String genre;
    private String language;
    private long publishDate = 0;
    private Collection<Author> authors = new ArrayList<>();
    private Publisher publisher = null;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public void setPublishDate(final long publishDate) {
        this.publishDate = publishDate;
    }

    public long getPublishDate() {
        return this.publishDate;
    }

    @ManyToMany
    public Collection<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Collection<Author> authors) {
        this.authors = authors;
    }

    @OneToOne
    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    @OnUpdatePreSecurity("title")
    public void onUpdateTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @OnCreatePreSecurity
    public void onCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @OnDeletePreSecurity
    public void onDeleteBook(RequestScope requestScope) {
        // book entity deleted
    }

    @OnUpdatePreCommit("title")
    public void preUpdateTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @OnCreatePreCommit
    public void preCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @OnDeletePreCommit
    public void preDeleteBook(RequestScope requestScope) {
        // book entity deleted
    }

    @OnUpdatePostCommit("title")
    public void postUpdateTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @OnCreatePostCommit
    public void postCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @OnDeletePostCommit
    public void postDeleteBook(RequestScope requestScope) {
        // book entity deleted
    }

    @OnReadPreSecurity
    public void preRead(RequestScope requestScope) {
        // book being read pre security
    }

    @OnReadPreCommit("title")
    public void preCommitRead(RequestScope requestScope) {
        // book being read pre commit
    }

    @OnReadPostCommit
    public void postRead(RequestScope requestScope) {
        // book being read post commit
    }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "\'}";
    }
}
