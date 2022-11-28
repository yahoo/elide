/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/**
 * A model intended to be ONLY created and read, but never updated
 */
@Include
@Entity
@CreatePermission(expression = "Prefab.Role.All")
public class CreateButNoReadChild extends BaseId {
    private CreateButNoRead otherObject;

    @ManyToOne()
    @ReadPermission(expression = "Prefab.Role.All")
    public CreateButNoRead getOtherObject() {
        return otherObject;
    }

    public void setOtherObject(CreateButNoRead otherObject) {
        this.otherObject = otherObject;
    }
}
