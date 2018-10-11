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
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
@CreatePermission(expression = "filterCheck")
@ReadPermission(expression = "deny all OR filterCheck OR filterCheck3")
@UpdatePermission(expression = "filterCheck")
@DeletePermission(expression = "filterCheck")
@Include(rootLevel = true)
// Hibernate
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@ToString
public class Filtered  {
    @ReadPermission(expression = "deny all") public transient boolean init = false;

    private long id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    static private FilterPredicate getPredicateOfId(long id) {
        Path.PathElement path1 = new Path.PathElement(Filtered.class, long.class, "id");
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add(id);
        return new FilterPredicate(path1, op, value);
    }

    /**
     * Filter for ID == 1.
     */
    static public class FilterCheck extends FilterExpressionCheck {
        /* Limit reads to ID 1 */
        @Override
        public FilterExpression getFilterExpression(Class entityClass, RequestScope requestScope) {
            return getPredicateOfId(1L);
        }
    }

    /**
     * Filter for ID == 3.
     */
    static public class FilterCheck3 extends FilterExpressionCheck {
        /* Limit reads to ID 3 */
        @Override
        public FilterExpression getFilterExpression(Class entityClass, RequestScope requestScope) {
            return getPredicateOfId(3L);

        }
    }
}
