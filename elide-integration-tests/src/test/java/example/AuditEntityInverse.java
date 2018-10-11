/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
@Include(rootLevel = true)
@ReadPermission(expression = "allow all")
@CreatePermission(expression = "allow all")
@UpdatePermission(expression = "allow all")
@DeletePermission(expression = "allow all")
@SharePermission
public class AuditEntityInverse extends BaseId {
    private List<AuditEntity> entities;

    @ManyToMany
    @Audit(action = Audit.Action.UPDATE,
            logStatement = "Inverse entities: {0}",
            logExpressions = "${auditEntityInverse.entities}")
    public List<AuditEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<AuditEntity> entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return "AuditEntityInverse{"
                + "id=" + id
                + ", entities=" + entities
                + '}';
    }
}
