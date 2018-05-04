/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import javax.persistence.Entity;

@ReadPermission(expression = "allow all")
@CreatePermission(expression = "allow all")
@UpdatePermission(expression = "allow all")
@Entity
public class NotIncludedResource extends BaseId {
    private String someParams;

    public String getSomeParams() {
        return someParams;
    }

    public void setSomeParams(String someParams) {
        this.someParams = someParams;
    }
}
