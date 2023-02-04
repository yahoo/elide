/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.generics;

import jakarta.persistence.Entity;

/**
 * Tests a parameterized superclass.
 */
@Entity
public class Manager extends Overlord<Employee> {
}
