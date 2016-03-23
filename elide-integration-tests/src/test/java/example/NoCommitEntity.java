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
import com.yahoo.elide.security.checks.prefab.Role;
import example.NoCommitEntity.NoCommitCheck;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.Optional;

/**
 * No Update test bean.
 */
@CreatePermission(all = { Role.ALL.class, NoCommitCheck.class })
@UpdatePermission(all = { Role.ALL.class, NoCommitCheck.class })
@Include(rootLevel = true, type = "nocommit")
// Hibernate
@Entity
@Table(name = "nocommit")
public class NoCommitEntity {
    static public class NoCommitCheck<T> extends CommitCheck<T> {
        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }

    private long id;

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
