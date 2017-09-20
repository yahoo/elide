/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Include(rootLevel = true, type = "smartphone")
@SharePermission(expression = "allow all")
public class Smartphone extends BaseId implements Device {
    @Getter @Setter private String type;
}
