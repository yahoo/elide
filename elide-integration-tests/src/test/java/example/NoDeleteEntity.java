/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * No Delete test bean.
 */
@DeletePermission(expression = "Prefab.Role.None")
@Include(name = "nodelete") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "nodelete")
public class NoDeleteEntity extends BaseId {
}
