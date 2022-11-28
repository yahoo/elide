/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;


@Include(name = "right") // optional here because class has this name
@UpdatePermission(expression = "Prefab.Role.None")
@Entity
@Table(name = "xright")     // right is SQL keyword
public class Right {
    @JsonIgnore
    private long id;
    private Left many2one;
    private Left one2one;
    private Set<Left> allowDeleteAtFieldLevel;
    private Left noUpdateOne2One;
    private Set<Left> noUpdate;

    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    @UpdatePermission(expression = "Prefab.Role.All")
    public Left getOne2one() {
        return one2one;
    }

    public void setOne2one(Left one2one) {
        this.one2one = one2one;
    }

    public void setMany2one(Left many2one) {
        this.many2one = many2one;
    }

    @ManyToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Left getMany2one() {
        return many2one;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    @UpdatePermission(expression = "Prefab.Role.None")
    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Left getNoUpdateOne2One() {
        return noUpdateOne2One;
    }

    public void setNoUpdateOne2One(Left noUpdateOne2One) {
        this.noUpdateOne2One = noUpdateOne2One;
    }

    @UpdatePermission(expression = "Prefab.Role.None")
    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Set<Left> getNoUpdate() {
        return noUpdate;
    }

    public void setNoUpdate(Set<Left> noUpdate) {
        this.noUpdate = noUpdate;
    }

    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    @UpdatePermission(expression = "Prefab.Role.All")
    public Set<Left> getAllowDeleteAtFieldLevel() {
        return allowDeleteAtFieldLevel;
    }

    public void setAllowDeleteAtFieldLevel(Set<Left> allowDeleteAtFieldLevel) {
        this.allowDeleteAtFieldLevel = allowDeleteAtFieldLevel;
    }
}
