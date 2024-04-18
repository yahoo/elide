/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.paiondata.elide.annotation.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
@Include(rootLevel = false, description = "The Author", friendlyName = "Author")
public class Author {

    public AuthorType membershipType;

    public String name;

    @Schema(requiredMode = RequiredMode.REQUIRED)
    public String phone;

    @OneToMany
    public Set<Book> getBooks() {
        return null;
    }

    @ManyToOne
    public Agent getAgent() {
        return null;
    }

    @OneToMany
    public Set<Publisher> getPublisher() {
        return null;
    }
}
