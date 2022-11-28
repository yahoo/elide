/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.None OR Prefab.Role.All")
@DeletePermission(expression = "Prefab.Role.None AND Prefab.Role.All")
@Include(name = "fun") // optional here because class has this name
@Entity
@Table(name = "fun")
public class FunWithPermissions {
    @JsonIgnore
    private long id;
    private String field1;
    private String field2;
    private String field3;
    private String field4;
    private Set<Child> relation1;
    private Set<Child> relation2;
    private Child relation3;

    @ReadPermission(expression = "negativeIntegerUser")
    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    @UpdatePermission(expression = "negativeIntegerUser")
    public String getField4() {
        return field4;
    }

    public void setField4(String field4) {
        this.field4 = field4;
    }

    public void setRelation2(Set<Child> relation2) {
        this.relation2 = relation2;
    }

    @ReadPermission(expression = "negativeIntegerUser")
    @OneToMany(
            targetEntity = Child.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    public Set<Child> getRelation2() {
        return relation2;
    }

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

    @ReadPermission(expression = "Prefab.Role.None")
    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    @ReadPermission(expression = "Prefab.Role.All")
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

    private String field5;

    @ReadPermission(expression = "Prefab.Role.All OR negativeIntegerUser")
    public String getField5() {
        return field5;
    }

    public void setField5(String field5) {
        this.field5 = field5;
    }

    private String field6;

    @ReadPermission(expression = "negativeIntegerUser AND Prefab.Role.All")
    public String getField6() {
        return field6;
    }

    public void setField6(String field6) {
        this.field6 = field6;
    }

    private String field7;

    /* Verifies a chain of checks where the last can fail. */
    @ReadPermission(expression = "Prefab.Role.All AND Prefab.Role.None")
    public String getField7() {
        return field7;
    }

    public void setField7(String field7) {
        this.field7 = field7;
    }

    private String field8;

    /* Verifies a chain of checks where all can fail or the last can succeed. */
    @ReadPermission(expression = "Prefab.Role.None OR negativeIntegerUser")
    public String getField8() {
        return field8;
    }

    public void setField8(String field8) {
        this.field8 = field8;
    }
}
