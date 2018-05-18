/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Entity
@Include(rootLevel = true, type = "specialread")
@ReadPermission(expression = "specialValue")
@UpdatePermission(expression = "updateOnCreate")
public class SpecialRead extends BaseId {
    public String value;

    private Child child;

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
            for (PersistentResource resource : requestScope.getNewResources()) {
                if (resource.getObject() == object) {
                    return "special value".equals(object.value);
                }
            }
            return false;
        }
    }
}
