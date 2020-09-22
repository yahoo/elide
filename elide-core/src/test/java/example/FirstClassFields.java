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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@CreatePermission(expression = "Prefab.Role.None")
@ReadPermission(expression = "Prefab.Role.None")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Include
@Entity
public class FirstClassFields {
    @Id
    public Integer id;

    // Private vars as defined by entity
    public String private1;

    @OneToOne
    public Left private2;

    // Public vars
    @ReadPermission(expression = "allow all")
    public String public1;

    @ReadPermission(expression = "allow all")
    @OneToOne
    public Left public2;
}
