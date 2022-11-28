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

@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.None")
@Include
@Entity
public class YetAnotherPermission extends BaseId {
    private String hiddenName;
    private String youShouldBeAbleToRead;

    public String getHiddenName() {
        return hiddenName;
    }

    public void setHiddenName(String hiddenName) {
        this.hiddenName = hiddenName;
    }

    @ReadPermission(expression = "Prefab.Role.All")
    public String getYouShouldBeAbleToRead() {
        return youShouldBeAbleToRead;
    }

    public void setYouShouldBeAbleToRead(String youShouldBeAbleToRead) {
        this.youShouldBeAbleToRead = youShouldBeAbleToRead;
    }
}
