/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.prefab.Role;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import example.Filtered.FilterCheck;
import example.Filtered.FilterCheck3;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

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
public class Filtered  {
    @ReadPermission(all = { Role.NONE.class }) public transient boolean init = false;

    private long id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    static private Predicate getPredicateOfId(long id) {
        List<Predicate.PathElement> pathList = new ArrayList<>();
        Predicate.PathElement path1 = new Predicate.PathElement(Filtered.class, "filtered", long.class, "id");
        pathList.add(path1);
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add(id);
        return new Predicate(pathList, op, value);
    }

    /**
     * Filter for ID == 1.
     */
    static public class FilterCheck extends FilterExpressionCheck {
        /* Limit reads to ID 1 */
        @Override
        public Predicate getFilterExpression(RequestScope requestScope) {
            return getPredicateOfId(1L);
        }
    }

    /**
     * Filter for ID == 3.
     */
    static public class FilterCheck3 extends FilterExpressionCheck {
        /* Limit reads to ID 3 */
        @Override
        public Predicate getFilterExpression(RequestScope requestScope) {
            return getPredicateOfId(3L);

        }
    }
}
