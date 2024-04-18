/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.type.Type;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Model for anotherFilterExpressionCheckObj.
 */
@Entity
@Table(name = "anotherFilterExpressionCheckObj")
@ReadPermission(expression = "checkActsLikeFilter")
@Include
public class AnotherFilterExpressionCheckObj extends BaseId {
    private String anotherName;
    private long createDate = 0;

    private Collection<FilterExpressionCheckObj> linkToParent = new ArrayList<>();

    @ManyToMany
    public Collection<FilterExpressionCheckObj> getLinkToParent() {
        return linkToParent;
    }

    public void setLinkToParent(Collection<FilterExpressionCheckObj> linkToParent) {
        this.linkToParent = linkToParent;
    }

    public String getAnotherName() {
        return anotherName;
    }

    public void setAnotherName(String anotherName) {
        this.anotherName = anotherName;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public static FilterPredicate createFilterPredicate() {
        Path.PathElement path1 = new Path.PathElement(AnotherFilterExpressionCheckObj.class,
                long.class, "createDate");
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add(1999L);
        return new FilterPredicate(path1, op, value);
    }

    public static class CheckActsLikeFilter extends FilterExpressionCheck<Object> {

        @Override
        public FilterExpression getFilterExpression(Type<?> entityClass, RequestScope requestScope) {
            return createFilterPredicate();
        }

        public CheckActsLikeFilter() {

        }
    }
}
