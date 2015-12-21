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
import com.yahoo.elide.security.Access;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@CreatePermission(all = {Access.NONE.class})
@ReadPermission(all = {Access.NONE.class})
@UpdatePermission(all = {Access.NONE.class})
@DeletePermission(all = {Access.NONE.class})
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
    @ReadPermission(any = {Access.ALL.class})
    public String public1;

    @ReadPermission(any = {Access.ALL.class})
    @OneToOne
    public Left public2;
}
