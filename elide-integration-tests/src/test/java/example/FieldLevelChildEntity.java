/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "child_field_level")
@Include(name = "fieldLevelChild")
public class FieldLevelChildEntity extends FieldLevelParentSuperclass {
    private String childField;
}
