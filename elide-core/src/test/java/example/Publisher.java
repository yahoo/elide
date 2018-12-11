/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Model for publisher.
 */
@Entity
@Include
public class Publisher {
    private long id;
    private String name;
    private Set<Book> books = new HashSet<>();
    private Editor editor;

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

    public void setBooks(Set<Book> books) {
        this.books = books;
    }

    @OneToOne
    @FilterExpressionPath("editor")
    @ReadPermission(expression = "Field path editor check")
    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }
}
