/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Include(name = "product", rootLevel = false)
@Entity
public class ArtifactProduct {
    @Id
    private String name = "";

    private String commonName = "";

    private String description = "";

    @ManyToOne
    private ArtifactGroup group = null;

    @OneToMany(mappedBy = "artifact")
    private List<ArtifactVersion> versions = new ArrayList<>();
}
