/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.yahoo.elide.annotation.Include;

import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
@Include(rootLevel = false, description = "The Author")
public class Author {

    public AuthorType membershipType;

    public String name;

    @ApiModelProperty(required = true)
    public String phone;

    @OneToMany
    public Set<Book> getBooks() {
        return null;
    }

    @OneToMany
    public Set<Publisher> getPublisher() {
        return null;
    }
}
