/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Entity(name = "childEntity")
@CreatePermission(expression = "initCheck")
@ReadPermission(expression = "negativeChildId AND negativeIntegerUser AND initCheck")
@Include(name = "child")
@Audit(action = Audit.Action.DELETE,
       operation = 0,
       logStatement = "DELETE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
@Audit(action = Audit.Action.CREATE,
       operation = 0,
       logStatement = "CREATE Child {0} Parent {1}",
       logExpressions = {"${child.id}", "${parent.id}"})
public class Child implements WithMetadata {
    @JsonIgnore

    private long id;
    private Set<Parent> parents;

    private Map<String, Object> metadata = new HashMap<>();

    private String name;

    private Set<Child> friends;
    private Child noReadAccess;

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
    @ReadPermission(expression = "Prefab.Role.None")
    public Child getReadNoAccess() {
        return noReadAccess;
    }

    public void setReadNoAccess(Child noReadAccess) {
        this.noReadAccess = noReadAccess;
    }

    @Exclude
    @Transient
    @Override
    public void setMetadataField(String property, Object value) {
        metadata.put(property, value);
    }

    @Exclude
    @Transient
    @Override
    public Optional<Object> getMetadataField(String property) {
        return Optional.ofNullable(metadata.getOrDefault(property, null));
    }

    @Exclude
    @Transient
    @Override
    public Set<String> getMetadataFields() {
        return metadata.keySet();
    }

    static public class InitCheck extends OperationCheck<Child> {
        @Override
        public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return child.getParents() != null;
        }
    }
}
