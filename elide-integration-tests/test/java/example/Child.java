/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.Role;
import example.Child.InitCheck;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Set;

/**
 * Child test bean.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@CreatePermission(any = { InitCheck.class })
@SharePermission(any = { Role.ALL.class })
@ReadPermission(all = {NegativeChildIdCheck.class, NegativeIntegerUserCheck.class, InitCheck.class})
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

    /**
     * Initialization validation check.
     */
    static public class InitCheck implements Check<Child> {
        @Override
        public boolean ok(PersistentResource<Child> record) {
            Child child = record.getObject();
            if (child.getParents() != null) {
                return true;
            }
            throw new IllegalStateException("Uninitialized " + child);
        }
    }
}
