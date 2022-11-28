/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.UpdatePermission;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;

@Include(name = "left") // optional here because class has this name
@Entity
@Table(name = "xleft")  // left is SQL keyword
@DeletePermission(expression = "negativeIntegerUser")
@NonTransferable
public class Left {
    @JsonIgnore
    private long id;
    private Set<Right> one2many;
    private Right one2one;
    private NoDeleteEntity noDeleteOne2One;
    private Set<Right> fieldLevelDelete;
    private Right noUpdateOne2One;
    private Set<Right> noInverseUpdate;

    @OneToOne(
            optional = false,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Right.class,
            mappedBy = "one2one"
    )
    public Right getOne2one() {
        return one2one;
    }

    public void setOne2one(Right one2one) {
        this.one2one = one2one;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setOne2many(Set<Right> one2many) {
        this.one2many = one2many;
    }

    @OneToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Right.class,
            mappedBy = "many2one"
    )
    public Set<Right> getOne2many() {
        return one2many;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    @UpdatePermission(expression = "Prefab.Role.None")
    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Right.class,
            mappedBy = "noUpdateOne2One"
    )
    public Right getNoUpdateOne2One() {
        return noUpdateOne2One;
    }

    public void setNoUpdateOne2One(Right noUpdateOne2One) {
        this.noUpdateOne2One = noUpdateOne2One;
    }

    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
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
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = NoDeleteEntity.class

    )
    public NoDeleteEntity getNoDeleteOne2One() {
        return noDeleteOne2One;
    }

    public void setNoDeleteOne2One(NoDeleteEntity noDeleteOne2One) {
        this.noDeleteOne2One = noDeleteOne2One;
    }

    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    public Set<Right> getFieldLevelDelete() {
        return fieldLevelDelete;
    }

    public void setFieldLevelDelete(Set<Right> fieldLevelDelete) {
        this.fieldLevelDelete = fieldLevelDelete;
    }
}
