/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * No Update test bean.
 */
@CreatePermission(expression = "allow all AND noCommit")
@UpdatePermission(expression = "allow all AND noCommit")
@Include(rootLevel = true, type = "nocommit")
// Hibernate
@Entity
@Table(name = "nocommit")
public class NoCommitEntity extends BaseId {
    static public class NoCommitCheck<T> extends CommitCheck<T> {
        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }
}
