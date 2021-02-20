/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.generics;

import com.yahoo.elide.annotation.Exclude;

import javax.persistence.Entity;

/**
 * Helper class to test parameterized subclass/superclass hierarchies.
 */
@Entity
@Exclude
public class Other extends Peon<Manager> {
}
