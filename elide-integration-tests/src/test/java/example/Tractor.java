/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Include(rootLevel = true, type = "tractor")
@SharePermission
public class Tractor extends BaseId implements Device {
    @Getter @Setter private int horsepower;
}
