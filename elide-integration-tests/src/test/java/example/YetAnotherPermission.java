/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import javax.persistence.Entity;

@CreatePermission(expression = "allow all")
@ReadPermission(expression = "deny all")
@Include(rootLevel = true)
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

    @ReadPermission(expression = "allow all")
    public String getYouShouldBeAbleToRead() {
        return youShouldBeAbleToRead;
    }

    public void setYouShouldBeAbleToRead(String youShouldBeAbleToRead) {
        this.youShouldBeAbleToRead = youShouldBeAbleToRead;
    }
}
