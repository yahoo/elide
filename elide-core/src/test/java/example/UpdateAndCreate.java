/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Model for authors.
 */
@Entity
@Include(rootLevel = true)
@CreatePermission(expression = "Principal is user one OR Principal is user two")
@UpdatePermission(expression = "Principal is user two")
@SharePermission
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${author.name}"})
public class UpdateAndCreate {
    public enum AuthorType {
        EXCLUSIVE,
        CONTRACTED,
        FREELANCE
    }

    private Long id;
    private String name;
    private String alias;
    private Collection<Book> books = new ArrayList<>();
    private Author author;
    private AuthorType type;


    @Getter
    @Setter
    private Address homeAddress;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @CreatePermission(expression = "Principal is user one OR Principal is user two OR Principal is user three")
    @UpdatePermission(expression = "Principal is user two OR Principal is user four")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @CreatePermission(expression = "Principal is user two")
    public AuthorType getType() {
        return type;
    }

    public void setType(AuthorType type) {
        this.type = type;
    }

    @OneToMany
    public Collection<Book> getBooks() {
        return books;
    }

    public void setBooks(Collection<Book> books) {
        this.books = books;
    }

    @CreatePermission(expression = "Principal is user two")
    @UpdatePermission(expression = "Principal is user three")
    @OneToOne
    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
