/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;

import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Model for books.
 */
@Entity
@SharePermission
@Table(name = "book")
@Include(rootLevel = true)
@Paginate
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${book.title}"})
public class Book extends BaseId {
    private String title;
    private String genre;
    private String language;
    private long publishDate = 0;
    private Collection<Author> authors = new ArrayList<>();
    private Collection<Chapter> chapters = new ArrayList<>();
    private String editorName;
    private Publisher publisher;

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

    /**
     * Demonstrates a more complex ranking use case.
     * @return The number of chapters in a book.
     */
    @Formula(value = "(SELECT COUNT(*) FROM book AS b JOIN book_chapter AS bc ON bc.book_id = b.id WHERE id=b.id)")
    public int getChapterCount() {
        return chapters.size();
    }

    public void setChapterCount(int unused) {
        //NOOP
        return;
    }

    @OneToMany
    @JoinTable(
            name = "book_chapter",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "chapters_id")
    )
    public Collection<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(Collection<Chapter> chapters) {
        this.chapters = chapters;
    }

    @ManyToMany
    public Collection<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Collection<Author> authors) {
        this.authors = authors;
    }

    // Case sensitive collation for H2
    @Column(columnDefinition = "VARCHAR_CASESENSITIVE(255) DEFAULT NULL")
    public String getEditorName() {
        return editorName;
    }

    public void setEditorName(String editorName) {
        this.editorName = editorName;
    }

    @ManyToOne
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
        if (publisher != null) {
            return getPublisher().getEditor();
        }

        return null;
    }
}
