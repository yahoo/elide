/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;
import example.Author.AuthorType;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Model for books.
 */
@Entity
@CreatePermission(expression = "Book operation check")
@UpdatePermission(expression = "Book operation check")
@DeletePermission(expression = "Book operation check")
@Table(name = "book")
@Include
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${book.title}"})
@AllArgsConstructor
@NoArgsConstructor
public class Book implements Product {
    private long id;
    private String title;
    private String genre;
    private String language;
    private long publishDate = 0;
    private Collection<Author> authors = new ArrayList<>();
    private Publisher publisher = null;
    private Collection<String> awards = new ArrayList<>();
    private Price price;

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

    @ElementCollection(targetClass = AuthorType.class)
    @FilterExpressionPath("authors.type")
    public Collection<AuthorType> getAuthorTypes() {
        return getAuthors().stream().map(Author::getType).distinct().collect(Collectors.toList());
    }


    @ElementCollection(targetClass = String.class)
    public Collection<String> getAwards() {
        return awards;
    }

    public void setAwards(Collection<String> awards) {
        this.awards = awards;
    }

    @OneToOne
    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    @Transient
    @ComputedRelationship
    @OneToOne
    @FilterExpressionPath("publisher.editor")
    @ReadPermission(expression = "Field path editor check")
    public Editor getEditor() {
        return getPublisher().getEditor();
    }

    public void checkPermission(RequestScope requestScope) {
        // performs create permission check
    }

    static public class BookOperationCheck extends OperationCheck<Book> {
        @Override
        public boolean ok(Book book, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            // trigger method for testing
            book.checkPermission(requestScope);
            return true;
        }
    }
}
