/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Set;


@Include(rootLevel = true, type = "left") // optional here because class has this name
@Entity
@Table(name = "xleft")  // left is SQL keyword
@DeletePermission(expression = "negativeIntegerUser")
public class Left {
    @JsonIgnore
    private long id;
    private Set<Right> one2many;
    private Right one2one;

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

    @UpdatePermission(expression = "deny all")
    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Right.class,
            mappedBy = "noUpdateOne2One"
    )
    public Right noUpdateOne2One;

    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Right.class,
            mappedBy = "noUpdate"
    )
    public Set<Right> noInverseUpdate;

    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = NoDeleteEntity.class
    )
    public NoDeleteEntity noDeleteOne2One;

    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        targetEntity = Right.class,
        mappedBy = "allowDeleteAtFieldLevel"
    )
    public Set<Right> fieldLevelDelete;
}
