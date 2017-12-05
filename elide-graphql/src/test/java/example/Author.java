/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model for authors.
 */
@Entity
@Table(name = "author")
@Include(rootLevel = true)
@SharePermission
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
    private String name;
    private AuthorType type;
    private Address homeAddress;
    private String prefix = "default_";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthorType getType() {
        return type;
    }

    public void setType(AuthorType type) {
        this.type = type;
    }

    public Address getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(Address homeAddress) {
        this.homeAddress = homeAddress;
    }


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

    // TODO: Eventually we should support multiple argument computed attributes to be used as API _functions_
    @ComputedAttribute
    @Transient
    public List<String> getBookTitlesWithPrefix() {
        if (getBooks() == null) {
            return Collections.emptyList();
        }
        return getBooks().stream()
                .map(book -> getPrefix() + book.getTitle())
                .collect(Collectors.toList());
    }

    public void setBookTitlesWithPrefix(List<String> unused) {
        // Do nothing
        System.out.println("hm");
    }

    @Transient
    @ComputedAttribute
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null) {
            this.prefix = prefix;
        }
    }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "\'}";
    }
}
