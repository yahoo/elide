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

import javax.persistence.Entity;

/**
 * A model intended to be ONLY created and read, but never updated.
 */
@Include(rootLevel = true)
@Entity
@CreatePermission(expression = "allow all")
@ReadPermission(expression = "allow all")
@UpdatePermission(expression = "deny all")
public class CreateButNoUpdate extends BaseId {
    public String textValue;

    @CreatePermission(expression = "deny all")
    public String cannotModify = "unmodified";
}
