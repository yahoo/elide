/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Model for authors.
 */
@Entity
@Table(name = "author")
@Include(rootLevel = true)
@CreatePermission(expression = "isUserOne OR isUserTwo")
@UpdatePermission(expression = "isUserTwo")
@SharePermission(expression = "allow all")
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

    @CreatePermission(expression = "isUserOne OR isUserTwo OR isUserThree")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @CreatePermission(expression = "isUserTwo")
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

    @CreatePermission(expression = "isUserTwo")
    @OneToOne
    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
