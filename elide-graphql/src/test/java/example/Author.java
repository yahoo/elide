/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Model for authors.
 * <p>
 * <b>CAUTION: DO NOT DECORATE IT WITH {@link Builder}, which hides its no-args constructor. This will result in
 * runtime error at places such as {@code entityClass.newInstance();}</b>
 */
@Entity
@Table(name = "author")
@Include
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${author.name}"})
@Subscription
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
    private AuthorType secondaryType;
    private Address homeAddress;
    private String prefix = "default_";
    private Date birthDate = null;
    private Set<PublicationFormat> publicationFormats = new HashSet<>();
    private Map<Book, PublicationFormat> publishedBookFormats = new HashMap<>();
    private Map<Book, PublicationFormat> favoriteBookFormats = new HashMap<>();
    private Object obj = "foo";
    private Map<PublicationFormat, Integer> booksPublishedByFormat = new HashMap<>();

    @SubscriptionField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    @SubscriptionField
    public AuthorType getType() {
        return type;
    }

    public void setType(AuthorType type) {
        this.type = type;
    }

    public AuthorType getSecondaryType() {
        return secondaryType;
    }

    public void setSecondaryType(AuthorType secondaryType) {
        this.secondaryType = secondaryType;
    }

    @SubscriptionField
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

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public Set<PublicationFormat> getPublicationFormats() {
        return publicationFormats;
    }

    public void setPublicationFormats(Set<PublicationFormat> publicationFormats) {
        this.publicationFormats = publicationFormats;
    }

    public Map<Book, PublicationFormat> getPublishedBookFormats() {
        return publishedBookFormats;
    }

    public void setPublishedBookFormats(Map<Book, PublicationFormat> publishedBookFormats) {
        this.publishedBookFormats = publishedBookFormats;
    }

    public Map<PublicationFormat, Integer> getBooksPublishedByFormat() {
        return booksPublishedByFormat;
    }

    public void setBooksPublishedByFormat(Map<PublicationFormat, Integer> booksPublishedByFormat) {
        this.booksPublishedByFormat = booksPublishedByFormat;
    }

    public Map<Book, PublicationFormat> getFavoriteBookFormats() {
        return favoriteBookFormats;
    }

    public void setFavoriteBookFormats(Map<Book, PublicationFormat> favoriteBookFormats) {
        this.favoriteBookFormats = favoriteBookFormats;
    }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "\'}";
    }
}
