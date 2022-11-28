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
import jakarta.persistence.PrimaryKeyJoinColumn;

@Include(rootLevel = false)
@ReadPermission(expression = "Prefab.Role.All")
@CreatePermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All")
@Entity
public class OneToOneNonRoot extends BaseId {
    private String test;

    private OneToOneRoot root;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    @OneToOne(mappedBy = "otherObject", fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    public OneToOneRoot getRoot() {
        return root;
    }

    public void setRoot(OneToOneRoot root) {
        this.root = root;
    }
}
