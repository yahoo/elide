/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Model for author book.
 */
@Entity
@Table(name = "permissionauthorbook")
@Include(name = "permissionauthorBook", description = "Author Book")
@Getter
@Setter
public class PermissionAuthorBook {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "AUTHOR_ID")
    private PermissionAuthor author;

    @ManyToOne
    @JoinColumn(name = "BOOK_ID")
    private PermissionBook book;
}
