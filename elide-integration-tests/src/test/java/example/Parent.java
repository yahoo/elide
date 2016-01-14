/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.Access;
import com.yahoo.elide.security.ChangeSpec;

import com.yahoo.elide.security.CommitCheck;
import com.yahoo.elide.security.OperationCheck;
import lombok.ToString;

import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;

/**
 * Parent test bean.
 */
@CreatePermission(any = { Parent.InitCheck.class, Access.ALL.class })
@ReadPermission(any = { Parent.InitCheck.class, Access.ALL.class })
@SharePermission(any = { Access.ALL.class })
@UpdatePermission(any = { Parent.InitCheck.class, Access.ALL.class, Access.NONE.class })
@DeletePermission(any = { Parent.InitCheck.class, Access.ALL.class, Access.NONE.class })
@Include(rootLevel = true, type = "parent") // optional here because class has this name
// Hibernate
@Entity
@ToString
public class Parent extends BaseId {
    private Set<Child> children;
    private Set<Parent> spouses;
    private String firstName;
    @ReadPermission(all = { Access.NONE.class }) public transient boolean init = false;

    public void doInit() {
        init = true;
    }

    @ReadPermission(any = { Access.ALL.class, Access.NONE.class })
    @UpdatePermission(any = { Access.ALL.class, Access.NONE.class })

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

    /**
     * Initialization validation check.
     */
    static public class InitCheck implements CommitCheck<Parent>, OperationCheck<Parent> {
        @Override
        public boolean ok(Parent parent, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (parent.getChildren() != null && parent.getSpouses() != null) {
                return true;
            }
            return false;
        }
    }
}
