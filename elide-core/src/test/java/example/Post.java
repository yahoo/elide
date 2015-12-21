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
import com.yahoo.elide.security.Access;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;

@CreatePermission(any = { Access.ALL.class })
@ReadPermission(any = { Access.ALL.class })
@UpdatePermission(any = { Access.ALL.class, Access.NONE.class })
@DeletePermission(any = { Access.ALL.class, Access.NONE.class })
@Include(rootLevel = true, type = "post") // optional here because class has this name
@Entity
public class Post extends BaseId {
    private String title;
    private int created;
    private final Set<Parent> spouses = new HashSet<>();

    @ReadPermission(all = { Access.NONE.class }) public transient boolean init = false;

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
