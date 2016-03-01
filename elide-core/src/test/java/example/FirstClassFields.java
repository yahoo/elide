/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@CreatePermission(all = {Role.NONE.class})
@ReadPermission(all = {Role.NONE.class})
@UpdatePermission(all = {Role.NONE.class})
@DeletePermission(all = {Role.NONE.class})
@Include(rootLevel = true)
@Entity
public class FirstClassFields {
    @Id
    public Integer id;

    // Private vars as defined by entity
    public String private1;

    @OneToOne
    public Left private2;

    // Public vars
    @ReadPermission(any = {Role.ALL.class})
    public String public1;

    @ReadPermission(any = {Role.ALL.class})
    @OneToOne
    public Left public2;
}
