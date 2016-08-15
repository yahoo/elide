/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.security.*;
import com.yahoo.elide.security.checks.prefab.Role;

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
@SharePermission(any = {Role.ALL.class})
@Table(name = "anotherFilterExpressionCheckObj")
@ReadPermission(any = {AnotherFilterExpressionCheckObj.CheckActsLikeFilter.class})
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

    public static Predicate createFilterPredicate() {
        List<Predicate.PathElement> pathList = new ArrayList<>();
        Predicate.PathElement path1 = new Predicate.PathElement(AnotherFilterExpressionCheckObj.class,
                "anotherFilterExpressionCheckObj",
                long.class, "createDate");
        pathList.add(path1);
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add(1999L);
        return new Predicate(pathList, op, value);
    }

    public static class CheckActsLikeFilter extends FilterExpressionCheck {

        @Override
        public Predicate getFilterExpression(RequestScope requestScope) {
            return createFilterPredicate();
        }

        public CheckActsLikeFilter() {

        }
    }
}
