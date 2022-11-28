/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.util.Optional;

@Entity
@Include(name = "specialread")
@ReadPermission(expression = "specialValue")
@UpdatePermission(expression = "Prefab.Role.None")
@CreatePermission(expression = "Prefab.Role.All")
public class SpecialRead extends BaseId {
    private String value;
    private Child child;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public Child getChild() {
        return child;
    }

    public void setChild(Child child) {
        this.child = child;
    }

    public static class SpecialValue extends OperationCheck<SpecialRead> {
        @Override
        public boolean ok(SpecialRead object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            // Only able to read new objects with the special value
            return "special value".equals(object.value);
        }
    }
}
