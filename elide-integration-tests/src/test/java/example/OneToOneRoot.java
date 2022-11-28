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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

@Include
@ReadPermission(expression = "Prefab.Role.All")
@CreatePermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All")
@Entity
public class OneToOneRoot extends BaseId {
    private String name;

    private OneToOneNonRoot otherObject;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToOne(fetch = FetchType.LAZY)
    public OneToOneNonRoot getOtherObject() {
        return otherObject;
    }

    public void setOtherObject(OneToOneNonRoot otherObject) {
        this.otherObject = otherObject;
    }
}
