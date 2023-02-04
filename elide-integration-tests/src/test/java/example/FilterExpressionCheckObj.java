/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Model for filterExpressionCheckObj.
 */
@Entity
@Table(name = "filterExpressionCheckObj")
@Include
@ReadPermission(expression = "checkLE OR Prefab.Role.None")  //ReadPermission for object id <= 2
public class FilterExpressionCheckObj extends BaseId {
    private String name;

    private Collection<AnotherFilterExpressionCheckObj> listOfAnotherObjs = new ArrayList<>();

    //This field only display for id == User.id (which is 1 in IT)
    @ReadPermission(expression = "checkRestrictUser")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToMany(mappedBy = "linkToParent")
    public Collection<AnotherFilterExpressionCheckObj> getListOfAnotherObjs() {
        return listOfAnotherObjs;
    }

    public void setListOfAnotherObjs(Collection<AnotherFilterExpressionCheckObj> listOfAnotherObjs) {
        this.listOfAnotherObjs = listOfAnotherObjs;
    }

    //Predicate that restrict resource's id to be the same as cuurent User's id.
    public static FilterPredicate createUserPredicate(RequestScope requestScope, boolean setUserId, long setId) {
        Path.PathElement path1 = new Path.PathElement(FilterExpressionCheckObj.class, long.class, "id");
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        int userId = Integer.valueOf(requestScope.getUser().getPrincipal().getName());
        if (setUserId) {
            value.add(setId);
        } else {
            value.add((long) userId);
        }
        return new FilterPredicate(path1, op, value);
    }

    public static class CheckRestrictUser extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Type entityClass, RequestScope requestScope) {
            return createUserPredicate(requestScope, false, 1L);
        }

        public CheckRestrictUser() {

        }
    }

    public static class CheckLE extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Type entityClass, RequestScope requestScope) {
            Path.PathElement path1 = new Path.PathElement(FilterExpressionCheckObj.class, long.class, "id");
            Operator op = Operator.LE;
            List<Object> value = new ArrayList<>();
            value.add((long) 2);
            return new FilterPredicate(path1, op, value);
        }

        public CheckLE() {

        }
    }
}
