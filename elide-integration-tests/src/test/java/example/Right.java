/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Right test bean.
 */
@Include(rootLevel = true, type = "right") // optional here because class has this name
@Entity
@Table(name = "xright")     // right is SQL keyword
public class Right {
    @JsonIgnore
    private long id;
    private Left many2one;
    private Left one2one;

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

    public void setId(long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    @UpdatePermission(expression = "deny all")
    @OneToOne(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class,
            fetch = FetchType.LAZY
    )
    public Left noUpdateOne2One;

    @UpdatePermission(expression = "deny all")
    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Set<Left> noUpdate;

    @ManyToMany(
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            targetEntity = Left.class
    )
    public Set<Left> noDelete;
}
