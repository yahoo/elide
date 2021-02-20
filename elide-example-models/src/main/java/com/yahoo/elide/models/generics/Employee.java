/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.generics;

import javax.persistence.Entity;

/**
 * Helper class to test parameterized subclass/superclass hierarchies.
 */
@Entity
public class Employee extends Peon<Manager> {
}
