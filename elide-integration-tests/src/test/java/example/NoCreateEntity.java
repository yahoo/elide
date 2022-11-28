/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * No Create test bean.
 */
@CreatePermission(expression = "Prefab.Role.None")
@Include(name = "nocreate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "nocreate")
public class NoCreateEntity extends BaseId {
}
