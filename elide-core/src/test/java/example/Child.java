/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.*;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.Role;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.checks.OperationCheck;
import example.Child.InitCheck;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.OneToOne;

@Entity
@CreatePermission(any = { InitCheck.class })
@SharePermission(any = {Role.ALL.class})
@ReadPermission(all = {NegativeChildIdCheck.class, NegativeIntegerUserCheck.class, Child.InitCheckOp.class})
@Include
@Audit(action = Audit.Action.DELETE,
       operation = 0,
       logStatement = "DELETE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
@Audit(action = Audit.Action.CREATE,
       operation = 0,
       logStatement = "CREATE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
public class Child {
    @JsonIgnore

    private long id;
    private Set<Parent> parents;


    private String name;

    private Set<Child> friends;
    private Child noReadAccess;

    public ExcludedEntity excludedEntity = new ExcludedEntity(1L, "excluded");

    @OneToOne
    public ExcludedEntity excludedRelationship = new ExcludedEntity(2L, "excluded2");

    public List<ExcludedEntity> excludedEntityList = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            mappedBy = "children",
            targetEntity = Parent.class
        )
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

    @OneToOne(
            targetEntity = Child.class
    )
    @ReadPermission(all = {Role.NONE.class})
    public Child getReadNoAccess() {
        return noReadAccess;
    }

    public void setReadNoAccess(Child noReadAccess) {
        this.noReadAccess = noReadAccess;
    }

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
}
