/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.models.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

/**
 * Example class used for testing.
 */
@Entity
@Include(rootLevel = true, type = "tractor")
@SharePermission
public class Tractor extends BaseId implements Device {
    @Getter @Setter private int horsepower;
}
