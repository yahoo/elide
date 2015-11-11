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
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.CriteriaCheck;
import com.yahoo.elide.security.Role;

import example.Filtered.FilterCheck;
import example.Filtered.FilterCheck3;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Filtered permission check.
 */
@CreatePermission(any = { FilterCheck.class })
@ReadPermission(any = { Role.NONE.class, FilterCheck.class, FilterCheck3.class })
@UpdatePermission(any = { FilterCheck.class })
@DeletePermission(any = { FilterCheck.class })
@Include(rootLevel = true)
// Hibernate
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@ToString
public class Filtered extends BaseId {
    private long id;
    @ReadPermission(all = { Role.NONE.class }) public transient boolean init = false;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Filter for ID == 1.
     */
    static public class FilterCheck implements CriteriaCheck<Filtered> {
        @Override
        public boolean ok(PersistentResource<Filtered> record) {
            return true;
        }

        /* Limit reads to ID 1 */
        @Override
        public Criterion getCriterion(RequestScope requestScope) {
            return Restrictions.idEq(1L);
        }
    }

    /**
     * Filter for ID == 3.
     */
    static public class FilterCheck3 implements CriteriaCheck<Filtered> {
        @Override
        public boolean ok(PersistentResource<Filtered> record) {
            return true;
        }

        /* Limit reads to ID 3 */
        @Override
        public Criterion getCriterion(RequestScope requestScope) {
            return Restrictions.idEq(3L);
        }
    }
}
