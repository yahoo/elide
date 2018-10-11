/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.models;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@Entity
@Include(rootLevel = true)
@ReadPermission(expression = "Principal is author OR Principal is publisher")
@CreatePermission(expression = "Principal is author")
@DeletePermission(expression = "Deny All")
public class Book {
    @OneToMany
    @Max(10)
    public Set<Author> getAuthors() {
        return null;
    }

    @OneToOne
    @UpdatePermission(expression = "Principal is publisher")
    public Publisher getPublisher() {
        return null;
    }

    @NotNull
    public String title;
}
