/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Model for anotherFilterExpressionCheckObj.
 */
@Entity
@SharePermission
@Table(name = "anotherFilterExpressionCheckObj")
@ReadPermission(expression = "checkActsLikeFilter")
@Include(rootLevel = true)
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

    public static class CheckActsLikeFilter extends FilterExpressionCheck {

        @Override
        public FilterExpression getFilterExpression(Class entityClass, RequestScope requestScope) {
            return createFilterPredicate();
        }

        public CheckActsLikeFilter() {

        }
    }
}
