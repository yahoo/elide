/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.generics;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;

/**
 * Helper class to test parameterized subclass/superclass hierarchies.
 */
@Include
@Entity
public class Employee extends Peon<Manager> {
}
