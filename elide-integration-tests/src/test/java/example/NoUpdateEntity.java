/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

/**
 * No Update test bean.
 */
@UpdatePermission(expression = "Prefab.Role.None")
@Include(name = "noupdate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noupdate")
public class NoUpdateEntity extends BaseId {
    @OneToMany
    protected Set<Child> children;
}
