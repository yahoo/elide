/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * left bean.
 */
@Include(rootLevel = true, type = "left") // optional here because class has this name
@Entity
@Table(name = "xleft")  // left is SQL keyword
@DeletePermission(expression = "negativeIntegerUser")
public class Left extends BaseId {
    private Set<Right> one2many;
    private Right one2one;
    private NoDeleteEntity noDeleteOne2One;
    private Set<Right> noInverseDelete;
    private Right noUpdateOne2One;
    private Set<Right> noInverseUpdate;

    @OneToOne(
            optional = false,
            targetEntity = Right.class,
            mappedBy = "one2one",
            fetch = FetchType.LAZY
    )
    public Right getOne2one() {
        return one2one;
    }

    public void setOne2one(Right one2one) {
        this.one2one = one2one;
    }

    public void setOne2many(Set<Right> one2many) {
        this.one2many = one2many;
    }

    @OneToMany(
            targetEntity = Right.class,
            mappedBy = "many2one"
    )
    public Set<Right> getOne2many() {
        return one2many;
    }

    @UpdatePermission(expression = "deny all")
    @OneToOne(
            targetEntity = Right.class,
            mappedBy = "noUpdateOne2One",
            fetch = FetchType.LAZY
    )
    public Right getNoUpdateOne2One() {
        return noUpdateOne2One;
    }

    public void setNoUpdateOne2One(Right noUpdateOne2One) {
        this.noUpdateOne2One = noUpdateOne2One;
    }

    @ManyToMany(
            targetEntity = Right.class,
            mappedBy = "noUpdate"
    )
    public Set<Right> getNoInverseUpdate() {
        return noInverseUpdate;
    }

    public void setNoInverseUpdate(Set<Right> noInverseUpdate) {
        this.noInverseUpdate = noInverseUpdate;
    }

    @OneToOne(
            targetEntity = NoDeleteEntity.class,
            fetch = FetchType.LAZY
    )
    public NoDeleteEntity getNoDeleteOne2One() {
        return noDeleteOne2One;
    }

    public void setNoDeleteOne2One(NoDeleteEntity noDeleteOne2One) {
        this.noDeleteOne2One = noDeleteOne2One;
    }

    @ManyToMany
    public Set<Right> getNoInverseDelete() {
        return noInverseDelete;
    }

    public void setNoInverseDelete(Set<Right> noInverseDelete) {
        this.noInverseDelete = noInverseDelete;
    }
}
