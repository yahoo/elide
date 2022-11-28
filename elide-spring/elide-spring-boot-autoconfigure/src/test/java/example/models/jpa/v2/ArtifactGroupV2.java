/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.jpa.v2;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Include(name = "group")
@Entity
@Data
@Table(name = "ArtifactGroup")
public class ArtifactGroupV2 {
    @Id
    private String name = "";

    @Column(name = "commonName")
    private String title = "";
}
