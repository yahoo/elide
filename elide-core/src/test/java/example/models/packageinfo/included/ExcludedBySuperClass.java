/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.packageinfo.included;

import example.ExcludedEntity;

import jakarta.persistence.Entity;

/**
 * Helper class to test Exclude on superclass
 */
@Entity
public class ExcludedBySuperClass extends ExcludedEntity {

    public ExcludedBySuperClass() {
        super(1, "");
    }
}
