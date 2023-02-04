/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Include(rootLevel = true, name = "group")
@Entity
public class ArtifactGroup {
    @Id
    private String name = "";

    @OneToMany(mappedBy = "group")
    private List<ArtifactProduct> products = new ArrayList<>();
}
