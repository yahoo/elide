/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.NotNullPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Child test bean.
 */
@Entity(name = "childEntity")
@CreatePermission(expression = "initCheck")
@SharePermission
@ReadPermission(expression = "negativeChildId AND negativeIntegerUser AND initCheckOp AND initCheckFilter")
@Include(rootLevel = true, type = "child")
@Audit(action = Audit.Action.DELETE,
       operation = 0,
       logStatement = "DELETE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
@Audit(action = Audit.Action.CREATE,
       operation = 0,
       logStatement = "CREATE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
public class Child extends BaseId {
    private Set<Parent> parents;

    private String name;

    private Set<Child> friends;
    private Child noReadAccess;

    @ManyToMany(
            mappedBy = "children",
            targetEntity = Parent.class
        )
    // Contrived check for regression example. Should clean this up. No updating child 4 via parent 10
    @UpdatePermission(expression = "child4Parent10")
    public Set<Parent> getParents() {
        return parents;
    }

    public void setParents(Set<Parent> parents) {
        this.parents = parents;
    }

    @ManyToMany(
            targetEntity = Child.class
    )
    public Set<Child> getFriends() {
        return friends;
    }

    public void setFriends(Set<Child> friends) {
        this.friends = friends;
    }

    @Audit(action = Audit.Action.UPDATE,
       operation = 1,
       logStatement = "UPDATE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"}
     )
    @Column(unique = true)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToOne(targetEntity = Child.class, fetch = FetchType.LAZY)
    @ReadPermission(expression = "deny all")
    public Child getNoReadAccess() {
        return noReadAccess;
    }

    public void setNoReadAccess(Child noReadAccess) {
        this.noReadAccess = noReadAccess;
    }

    @Transient
    @ComputedAttribute
    @ReadPermission(expression = "FailCheckOp")
    public String getComputedFailTest() {
        return "computed";
    }

    public String setComputedFailTest(String unused) {
        throw new IllegalAccessError();
    }

    /**
     * Initialization validation check.
     */
    static public class InitCheck extends CommitCheck<Child> {
        @Override
        public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (child.getParents() != null) {
                return true;
            }
            return false;
        }
    }

    static public class InitCheckOp extends OperationCheck<Child> {
        @Override
        public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (child.getParents() != null) {
                return true;
            }
            return false;
        }
    }

    static public class InitCheckFilter extends FilterExpressionCheck<Child> {
        @Override
        public FilterExpression getFilterExpression(Class<?> entityClass, RequestScope requestScope) {
            return new NotNullPredicate(new Path.PathElement(Child.class, Long.class, "id"));
        }
    }

    static public class FailCheckOp extends OperationCheck<Child> {
        @Override
        public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }
}
