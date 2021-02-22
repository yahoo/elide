/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.packageinfo;

import com.yahoo.elide.annotation.Exclude;

import example.models.BaseId;
import example.models.generics.Manager;
import example.models.generics.Peon;

import javax.persistence.Entity;

/**
 * Helper class to test parameterized subclass/superclass hierarchies.
 */
@Entity
public class IncludedPackageLevel extends BaseId {
}
