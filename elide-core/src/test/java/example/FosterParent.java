/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.security.checks.prefab.Role;
import lombok.ToString;

import javax.persistence.Entity;
import java.util.Set;

@Include(rootLevel = true, type = "foster", inheritPermissions = false)
@Entity
@ToString
public class FosterParent extends Parent {
    @Override
    @ReadPermission(any = {Role.ALL.class})
    public Set<Child> getChildren() {
        return super.getChildren();
    }
}
