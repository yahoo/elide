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
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Model for books.
 */
@Entity
@CreatePermission(expression = "Book operation check")
@UpdatePermission(expression = "Book operation check")
@DeletePermission(expression = "Book operation check")
@SharePermission
@Table(name = "book")
@Include(rootLevel = true)
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${book.title}"})
@AllArgsConstructor
public class Book {
    private long id;
    private String title;
    private String genre;
    private String language;
    private long publishDate = 0;
    private Collection<Author> authors = new ArrayList<>();
    private Publisher publisher = null;

    public Book() {

    }

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

    @Transient
    @ComputedRelationship
    @OneToOne
    @FilterExpressionPath("publisher.editor")
    @ReadPermission(expression = "Field path editor check")
    public Editor getEditor() {
        return getPublisher().getEditor();
    }

    @OnUpdatePreSecurity("title")
    public void onUpdatePreSecurityTitle(RequestScope requestScope) {
       // title attribute updated
    }

    @OnUpdatePreSecurity("genre")
    public void onUpdatePreSecurityGenre(RequestScope requestScope, ChangeSpec spec) {
       // genre attribute updated
    }

    @OnCreatePreSecurity
    public void onCreatePreSecurity(RequestScope requestScope) {
        // book entity created
    }

    @OnCreatePreCommit("*")
    public void onCreatePreCommitStar(RequestScope requestScope, ChangeSpec spec) {
        // book entity created
    }

    public void checkPermission(RequestScope requestScope) {
        // performs create permission check
    }

    @OnDeletePreSecurity
    public void onDeletePreSecurity(RequestScope requestScope) {
       // book entity deleted
    }

    @OnUpdatePreCommit("title")
    public void onUpdatePreCommitTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @OnUpdatePreCommit("genre")
    public void onUpdatePreCommitGenre(RequestScope requestScope, ChangeSpec spec) {
        // genre attribute updated
    }

    @OnCreatePreCommit
    public void onCreatePreCommit(RequestScope requestScope) {
        // book entity created
    }

    @OnDeletePreCommit
    public void onDeletePreCommit(RequestScope requestScope) {
        // book entity deleted
    }

    @OnUpdatePostCommit("title")
    public void onUpdatePostCommitTitle(RequestScope requestScope) {
        // title attribute updated
    }

    @OnUpdatePostCommit("genre")
    public void onUpdatePostCommitGenre(RequestScope requestScope, ChangeSpec spec) {
        // genre attribute updated
    }

    @OnCreatePostCommit
    public void onCreatePostCommit(RequestScope requestScope) {
        // book entity created
    }

    @OnDeletePostCommit
    public void onDeletePostCommit(RequestScope requestScope) {
        // book entity deleted
    }

    @OnReadPreSecurity
    public void onReadPreSecurity(RequestScope requestScope) {
        // book being read pre security
    }

    @OnReadPreCommit("title")
    public void onReadPreCommitTitle(RequestScope requestScope) {
        // book being read pre commit
    }

    @OnReadPostCommit
    public void onReadPostCommit(RequestScope requestScope) {
        // book being read post commit
    }

    @OnUpdatePreCommit
    public void onUpdatePreCommit() {
        // should be called on _any_ class update
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
