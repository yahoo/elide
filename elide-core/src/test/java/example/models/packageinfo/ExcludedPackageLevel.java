/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.packageinfo;

import com.yahoo.elide.annotation.Exclude;
import example.models.BaseId;

import jakarta.persistence.Entity;

/**
 * Helper class to test parameterized subclass/superclass hierarchies.
 */
@Entity
@Exclude
public class ExcludedPackageLevel extends BaseId {
}
