/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;

import javax.persistence.Entity;

/**
 * Post test bean.
 */
@CreatePermission(any = { Role.ALL.class })
@ReadPermission(any = { Role.ALL.class })
@UpdatePermission(any = { Role.ALL.class, Role.NONE.class })
@DeletePermission(any = { Role.ALL.class, Role.NONE.class })
@Include(rootLevel = true, type = "post") // optional here because class has this name
// Hibernate
@Entity
public class Post extends BaseId {
    private String title;
    private int created;

    @ReadPermission(all = { Role.NONE.class }) public transient boolean init = false;

    public void doInit() {
        init = true;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCreated() {
        return this.created;
    }

    public void setCreated(int created) {
        this.created = created;
    }
}
