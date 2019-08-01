/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.models.example;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Example class used for testing.
 */
@MappedSuperclass
public abstract class FieldLevelParentSuperclass {
    @Id
    private String id;
    private String parentField;
}
