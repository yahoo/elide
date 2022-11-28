/*
 * Copyright 2021, Yahoo.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Include(name = "product")
@Entity
public class ArtifactProduct {
    @Id
    private String name = "";

    @ManyToOne
    private ArtifactGroup group = null;
}
