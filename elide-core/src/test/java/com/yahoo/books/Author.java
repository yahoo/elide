/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.books;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import lombok.Getter;
import lombok.Setter;

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
 * Model for authors.
 */
@Entity
@Table(name = "author")
@Include(rootLevel = true)
@SharePermission(expression = "allow all")
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${author.name}"})
public class Author {
    public enum AuthorType {
        EXCLUSIVE,
        CONTRACTED,
        FREELANCE
    }

    private Long id;
    private Pseudonym penName;
    private Collection<Book> books = new ArrayList<>();

    @Getter @Setter
    private String name;
    @Getter @Setter
    private AuthorType type;
    @Getter @Setter
    private Address homeAddress;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @ManyToMany(mappedBy = "authors")
    public Collection<Book> getBooks() {
        return books;
    }
    public void setBooks(Collection<Book> books) {
        this.books = books;
    }

    @OneToOne(mappedBy = "author")
    public Pseudonym getPenName() {
        return penName;
    }
    public void setPenName(Pseudonym penName) {
        this.penName = penName;
    }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "\'}";
    }
}
