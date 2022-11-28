/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.Optional;
import java.util.Set;

/**
 * A model intended to be ONLY created and read, but never updated
 */
@Include
@Entity
@CreatePermission(expression = "Prefab.Role.All")
public class CreateButNoRead extends BaseId {
    private Set<CreateButNoReadChild> otherObjects;

    @OneToMany(fetch = FetchType.LAZY)
    @ReadPermission(expression = "noRead")
    public Set<CreateButNoReadChild> getOtherObjects() {
        return otherObjects;
    }

    public void setOtherObjects(Set<CreateButNoReadChild> otherObjects) {
        this.otherObjects = otherObjects;
    }

    public static class NOREAD extends OperationCheck<CreateButNoRead> {
        @Override
        public boolean ok(CreateButNoRead object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }
}
