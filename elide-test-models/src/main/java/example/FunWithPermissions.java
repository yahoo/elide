/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@CreatePermission(expression = "allow all")
@ReadPermission(expression = "allow all")
@UpdatePermission(expression = "deny all OR allow all")
@DeletePermission(expression = "deny all AND allow all")
@Include(rootLevel = true, type = "fun") // optional here because class has this name
@Entity
@Table(name = "fun")
public class FunWithPermissions {
    @JsonIgnore
    private long id;
    private String field1;
    private String field2;

    @ReadPermission(expression = "negativeIntegerUser")
    public String field3;

    @UpdatePermission(expression = "negativeIntegerUser")
    public String field4;

    private Set<Child> relation1;

    @ReadPermission(expression = "negativeIntegerUser")
    @OneToMany(
            targetEntity = Child.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    public Set<Child> relation2;


    private Child relation3;

    @ReadPermission(expression = "negativeIntegerUser")
    @UpdatePermission(expression = "negativeIntegerUser")
    @OneToOne (cascade = CascadeType.ALL)
    @JoinColumn(name = "child_id")
    public Child getRelation3() {
        return relation3;
    }

    public void setRelation3(Child relation3) {
        this.relation3 = relation3;
    }


    private Set<NoReadEntity> relation4;

    @ReadPermission(expression = "negativeIntegerUser")
    @OneToMany(
            targetEntity = NoReadEntity.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public Set<NoReadEntity> getRelation4() {
        // Permission should block access to this collection
        return new AbstractSet<NoReadEntity>() {
            @Override
            public Iterator<NoReadEntity> iterator() {
                // do not allow iteration
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void setRelation4(Set<NoReadEntity> relation4) {
        this.relation4 = relation4;
    }

    private NoReadEntity relation5;

    @ReadPermission(expression = "negativeIntegerUser")
    @OneToOne(
            targetEntity = NoReadEntity.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public NoReadEntity getRelation5() {
        // Permission should block access to this collection
        throw new UnsupportedOperationException();
    }

    public void setRelation5(NoReadEntity relation5) {
        this.relation5 = relation5;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ReadPermission(expression = "deny all")
    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    @ReadPermission(expression = "allow all")
    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    @ReadPermission(expression = "negativeIntegerUser")
    @UpdatePermission(expression = "negativeIntegerUser")
    @OneToMany(
            targetEntity = Child.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    public Set<Child> getRelation1() {
        return relation1;
    }

    public void setRelation1(Set<Child> relation) {
        this.relation1 = relation;
    }

    /* Verifies a chain of checks where all can succeed */
    @ReadPermission(expression = "allow all OR negativeIntegerUser")
    public String field5;

    /* Verifies a chain of checks where the first can fail or all succeed */
    @ReadPermission(expression = "negativeIntegerUser AND allow all")
    public String field6;

    /* Verifies a chain of checks where the last can fail. */
    @ReadPermission(expression = "allow all AND deny all")
    public String field7;

    /* Verifies a chain of checks where all can fail or the last can succeed. */
    @ReadPermission(expression = "deny all OR negativeIntegerUser")
    public String field8;
}
