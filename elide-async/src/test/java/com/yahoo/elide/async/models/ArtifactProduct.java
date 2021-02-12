/*
 * Copyright 2021, Verizon Media.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Include(type = "product")
@Entity
public class ArtifactProduct {
    @Id
    private String name = "";

    @ManyToOne
    private ArtifactGroup group = null;
}
