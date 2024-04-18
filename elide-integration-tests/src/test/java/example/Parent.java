/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.Paginate;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.OperationCheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotNull;
import lombok.ToString;

import java.util.Optional;
import java.util.Set;

/**
 * Parent test bean.
 */
@CreatePermission(expression = "parentInitCheck OR Prefab.Role.All")
@ReadPermission(expression = "parentInitCheck OR Prefab.Role.All")
@UpdatePermission(expression = "parentInitCheck OR Prefab.Role.All OR Prefab.Role.None")
@DeletePermission(expression = "parentInitCheck OR Prefab.Role.All OR Prefab.Role.None")
@Include(name = "parent") // optional here because class has this name
@Paginate(maxPageSize = 100000)
// Hibernate
@Entity
@ToString
public class Parent extends BaseId {
    private Set<Child> children;
    private Set<Parent> spouses;
    private String firstName;
    private String specialAttribute;
    @ReadPermission(expression = "Prefab.Role.None") public transient boolean init = false;

    public void doInit() {
        init = true;
    }

    @ReadPermission(expression = "Prefab.Role.All OR Prefab.Role.None")
    @UpdatePermission(expression = "Prefab.Role.All OR Prefab.Role.None")
    // Hibernate
    @ManyToMany(
            targetEntity = Child.class
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
    @ReadPermission(expression = "Prefab.Role.None")
    @UpdatePermission(expression = "parentSpecialValue")
    public String getSpecialAttribute() {
        return specialAttribute;
    }

    public void setSpecialAttribute(String specialAttribute) {
        this.specialAttribute = specialAttribute;
    }


    static public class InitCheck extends OperationCheck<Parent> {
        @Override
        public boolean ok(Parent parent, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return parent.getChildren() != null && parent.getSpouses() != null;
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
