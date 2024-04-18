/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;

@ReadPermission(expression = "Prefab.Role.All")
@CreatePermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All")
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
