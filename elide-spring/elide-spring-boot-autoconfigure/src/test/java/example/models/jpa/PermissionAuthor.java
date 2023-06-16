/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;


import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Model for author.
 * <p>
 * Used for testing deferred inline checks for
 * {@link example.checks.AuthorHasUserAccessibleBooks}.
 */
@Entity
@Table(name = "permissionauthor")
@Include(name = "permissionauthor", description = "Author")
@Getter
@Setter
@ReadPermission(expression = "author has user accessible books")
public class PermissionAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String name;

    @OneToMany(mappedBy = "author")
    private List<PermissionAuthorBook> authorBooks;
}
