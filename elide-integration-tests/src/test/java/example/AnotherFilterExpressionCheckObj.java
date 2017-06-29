/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Model for anotherFilterExpressionCheckObj.
 */
@Entity
@SharePermission(expression = "allow all")
@Table(name = "anotherFilterExpressionCheckObj")
@ReadPermission(expression = "checkActsLikeFilter")
@Include(rootLevel = true)
public class AnotherFilterExpressionCheckObj {
    private long id;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static FilterPredicate createFilterPredicate() {
        List<FilterPredicate.PathElement> pathList = new ArrayList<>();
        FilterPredicate.PathElement path1 = new FilterPredicate.PathElement(AnotherFilterExpressionCheckObj.class,
                "anotherFilterExpressionCheckObj",
                long.class, "createDate");
        pathList.add(path1);
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add(1999L);
        return new FilterPredicate(pathList, op, value);
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
