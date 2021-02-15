/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.annotation.ReadPermission;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Model for forbidden book.
 * No Details/Relationships etc.
 */
@Entity
@Table(name = "forbiddenbook")
@Include
@Paginate
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${forbiddenBook.title}"})
@ReadPermission(expression = "Prefab.Role.None")
public class ForbiddenBook extends BaseId {
    private String title;
    private String genre;
    private String language;

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

    @Override
    public String toString() {
        return "ForbiddenBook: " + id;
    }
}
