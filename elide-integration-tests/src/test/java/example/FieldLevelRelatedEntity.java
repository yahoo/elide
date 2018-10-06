/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "related_field_level")
@Include(rootLevel = true)
public class FieldLevelRelatedEntity extends FieldLevelParentSuperclass {
    private String name;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_child_id")
    private FieldLevelChildEntity relatedChild;
}
