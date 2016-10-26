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
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.checks.OperationCheck;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;

@CreatePermission(expression = "parentInitCheck OR allow all")
@ReadPermission(expression = "parentInitCheckOp OR allow all")
@UpdatePermission(expression = "parentInitCheck OR allow all OR deny all")
@DeletePermission(expression = "parentInitCheckOp OR allow all OR deny all")
@Include(rootLevel = true, type = "parent") // optional here because class has this name
@Entity
@ToString
public class Parent extends BaseId {
    private Set<Child> children;
    private Set<Parent> spouses;
    private String firstName;
    private String specialAttribute;
    @ReadPermission(expression = "deny all") public transient boolean init = false;

    @PrePersist
    public void doInit() {
        init = true;
    }

    @ReadPermission(expression = "allow all OR deny all")
    @UpdatePermission(expression = "allow all OR deny all")
    // Hibernate
    @ManyToMany(
            targetEntity = Child.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    @JoinTable(
            name = "Parent_Child",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
    )
    @NotNull
    public Set<Child> getChildren() {
        return children;
    }

    public void setChildren(Set<Child> children) {
        this.children = children;
    }

    @ManyToMany(
            targetEntity = Parent.class
    )
    public Set<Parent> getSpouses() {
        return spouses;
    }

    public void setSpouses(Set<Parent> spouses) {
        this.spouses = spouses;
    }

    @Column(name = "name")
    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String name) {
        this.firstName = name;
    }

    // Special attribute is to catch a corner case for patch extension
    @ReadPermission(expression = "deny all")
    @UpdatePermission(expression = "specialValue")
    public String getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(String specialAttribute) {
        this.specialAttribute = specialAttribute;
    }

    static public class InitCheck extends CommitCheck<Parent> {
        @Override
        public String checkIdentifier() {
            return "parentInitCheck";
        }

        @Override
        public boolean ok(Parent parent, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (parent.getChildren() != null && parent.getSpouses() != null) {
                return true;
            }
            return false;
        }
    }

    static public class InitCheckOp extends OperationCheck<Parent> {
        @Override
        public boolean ok(Parent parent, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (parent.getChildren() != null && parent.getSpouses() != null) {
                return true;
            }
            return false;
        }

        @Override
        public String checkIdentifier() {
            return "parentInitCheckOp";
        }
    }

    static public class SpecialValue extends OperationCheck<Parent> {
        @Override
        public boolean ok(Parent object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent()) {
                String fieldName = changeSpec.get().getFieldName();
                switch (fieldName) {
                    case "specialAttribute":
                        return object.getSpecialAttribute().equals("this should succeed!");
                }
            }
            return false;
        }

        @Override
        public String checkIdentifier() {
            return "specialValue";
        }
    }
}
