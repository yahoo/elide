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
import com.yahoo.elide.security.checks.prefab.Role;
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

@CreatePermission(any = { Parent.InitCheck.class, Role.ALL.class })
@ReadPermission(any = { Parent.InitCheckOp.class, Role.ALL.class })
@UpdatePermission(any = { Parent.InitCheck.class, Role.ALL.class, Role.NONE.class })
@DeletePermission(any = { Parent.InitCheckOp.class, Role.ALL.class, Role.NONE.class })
@Include(rootLevel = true, type = "parent") // optional here because class has this name
@Entity
@ToString
public class Parent extends BaseId {
    private Set<Child> children;
    private Set<Parent> spouses;
    private String firstName;
    private String specialAttribute;
    @ReadPermission(all = { Role.NONE.class }) public transient boolean init = false;

    @PrePersist
    public void doInit() {
        init = true;
    }

    @ReadPermission(any = { Role.ALL.class, Role.NONE.class })
    @UpdatePermission(any = { Role.ALL.class, Role.NONE.class })
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
    @ReadPermission(all = {Role.NONE.class})
    @UpdatePermission(all = {SpecialValue.class})
    public String getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(String specialAttribute) {
        this.specialAttribute = specialAttribute;
    }

    static public class InitCheck extends CommitCheck<Parent> {
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
    }
}
