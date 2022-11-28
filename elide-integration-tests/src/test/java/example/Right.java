/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;

/**
 * Right test bean.
 */
@Include(name = "right") // optional here because class has this name
@Entity
@Table(name = "xright")     // right is SQL keyword
public class Right extends BaseId {
    private Left many2one;
    private Left one2one;
    private Left noUpdateOne2One;
    private Set<Left> noUpdate;
    private Set<Left> noDelete;

    @UpdatePermission(expression = "Prefab.Role.None")
    @OneToOne(
            targetEntity = Left.class,
            fetch = FetchType.LAZY
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
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Set<Left> getNoDelete() {
        return noDelete;
    }

    public void setNoDelete(Set<Left> noDelete) {
        this.noDelete = noDelete;
    }

    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class,
            fetch = FetchType.LAZY

    )
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
            targetEntity = Left.class,
            fetch = FetchType.LAZY
    )
    public Left getMany2one() {
        return many2one;
    }
}
