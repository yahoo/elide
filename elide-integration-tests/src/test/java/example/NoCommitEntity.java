/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/**
 * No Update test bean.
 */
@CreatePermission(expression = "Prefab.Role.All AND noCommit")
@UpdatePermission(expression = "Prefab.Role.All AND noCommit")
@Include(name = "nocommit")
// Hibernate
@Entity
@Table(name = "nocommit")
public class NoCommitEntity extends BaseId {
    @Getter @Setter
    private String value;

    static public class NoCommitCheck<T> extends OperationCheck<T> {
        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }
}
