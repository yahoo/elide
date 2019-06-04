/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.jpa.v2;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Include(rootLevel = true, type = "group")
@Entity
@Data
@Table(name = "ArtifactGroup")
public class ArtifactGroupV2 {
    @Id
    private String name = "";

    @Column(name = "commonName")
    private String title = "";
}
