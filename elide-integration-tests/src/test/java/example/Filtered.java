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
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Entity;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtered permission check.
 */
@CreatePermission(expression = "filterCheck")
@ReadPermission(expression = "Prefab.Role.None OR filterCheck OR filterCheck3 OR negativeIntegerUser")
@UpdatePermission(expression = "filterCheck")
@DeletePermission(expression = "filterCheck")
@Include
// Hibernate
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@ToString
public class Filtered extends BaseId {
    @ReadPermission(expression = "Prefab.Role.None") public transient boolean init = false;

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
    static public class FilterCheck<T> extends FilterExpressionCheck<T> {
        /* Limit reads to ID 1 */
        @Override
        public FilterExpression getFilterExpression(Type entityClass, RequestScope requestScope) {
            return getPredicateOfId(1L);
        }
    }

    /**
     * Filter for ID == 3.
     */
    static public class FilterCheck3<T> extends FilterExpressionCheck<T> {
        /* Limit reads to ID 3 */
        @Override
        public FilterExpression getFilterExpression(Type entityClass, RequestScope requestScope) {
            return getPredicateOfId(3L);
        }
    }
}
