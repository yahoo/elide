/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.yahoo.elide.annotation.Include;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Include(name = "version", rootLevel = false)
@Entity
public class ArtifactVersion {
    @Id
    private String name = "";

    private Date createdAt = new Date();

    @ManyToOne
    private ArtifactProduct artifact;
}
