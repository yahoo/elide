/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;
import com.yahoo.elide.security.checks.prefab.UpdateOnCreateCheck;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * A model intended to be ONLY created and read, but never updated
 */
@Include(rootLevel = true)
@Entity
@CreatePermission(any = {Role.ALL.class})
@ReadPermission(any = {Role.ALL.class})
@UpdatePermission(any = {UpdateOnCreateCheck.class, Role.NONE.class})
public class CreateButNoUpdate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String textValue;

    @UpdatePermission(all = {Role.NONE.class})
    public String cannotModify = "unmodified";
}
