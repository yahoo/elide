/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Model for books.
 */
@Entity
@Table(name = "permissionbook")
@Include(name = "permissionbook", description = "A Book")
@Getter
@Setter
public class PermissionBook {
    @Id
    private long id;
    private String title;

    @OneToMany(mappedBy = "book")
    private List<PermissionAuthorBook> authorBooks;
}
