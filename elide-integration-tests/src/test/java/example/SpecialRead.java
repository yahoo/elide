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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Entity
@Include(rootLevel = true, type = "specialread")
@ReadPermission(expression = "specialValue")
@UpdatePermission(expression = "updateOnCreate")
public class SpecialRead {
    public Long id;

    public String value;

    @ManyToOne(fetch = FetchType.LAZY)
    public Child child;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
