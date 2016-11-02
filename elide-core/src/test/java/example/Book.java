/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreate;
import com.yahoo.elide.annotation.OnDelete;
import com.yahoo.elide.annotation.OnUpdate;
import com.yahoo.elide.annotation.PostCreate;
import com.yahoo.elide.annotation.PostDelete;
import com.yahoo.elide.annotation.PostUpdate;
import com.yahoo.elide.annotation.PreCreate;
import com.yahoo.elide.annotation.PreDelete;
import com.yahoo.elide.annotation.PreUpdate;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.RequestScope;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Model for books.
 */
@Entity
@SharePermission(expression = "allow all")
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
    private boolean onCreateBookCalled = false;

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

    @OnUpdate("title")
    public void onUpdateTitle(RequestScope requestScope) {
       // title attribute updated
    }

    @OnCreate
    public void onCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @OnDelete
    public void onDeleteBook(RequestScope requestScope) {
       // book entity deleted
    }

    @PreUpdate("title")
    public void preUpdateTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @PreCreate
    public void preCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @PreDelete
    public void preDeleteBook(RequestScope requestScope) {
        // book entity deleted
    }

    @PostUpdate("title")
    public void postUpdateTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @PostCreate
    public void postCreateBook(RequestScope requestScope) {
        // book entity created
    }

    @PostDelete
    public void postDeleteBook(RequestScope requestScope) {
        // book entity deleted
    }
}
