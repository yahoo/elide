/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.generics;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;

@Include(rootLevel = true)
@Entity
public class Manager extends Person<Manager, Employee> {
}
