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
import com.yahoo.elide.security.Role;

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

@CreatePermission(any = { Role.ALL.class })
@ReadPermission(all = { Role.ALL.class })
@UpdatePermission(any = { Role.NONE.class, Role.ALL.class })
@DeletePermission(all = { Role.ALL.class, Role.NONE.class })
@Include(rootLevel = true, type = "fun") // optional here because class has this name
@Entity
@Table(name = "fun")
public class FunWithPermissions {
    @JsonIgnore
    private long id;
    private String field1;
    private String field2;

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
    public String field3;

    @UpdatePermission(any = { NegativeIntegerUserCheck.class })
    public String field4;

    private Set<Child> relation1;

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
    @OneToMany(
            targetEntity = Child.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }
    )
    public Set<Child> relation2;


    private Child relation3;

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
    @UpdatePermission(any = { NegativeIntegerUserCheck.class })
    @OneToOne (cascade = CascadeType.ALL)
    @JoinColumn(name = "child_id")
    public Child getRelation3() {
        return relation3;
    }

    public void setRelation3(Child relation3) {
        this.relation3 = relation3;
    }


    private Set<NoReadEntity> relation4;

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
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

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
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

    @ReadPermission(any = { Role.NONE.class})
    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    @ReadPermission(all = { Role.ALL.class})
    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    @ReadPermission(any = { NegativeIntegerUserCheck.class })
    @UpdatePermission(any = { NegativeIntegerUserCheck.class })
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
    @ReadPermission(any = { NegativeIntegerUserCheck.class, Role.ALL.class })
    public String field5;

    /* Verifies a chain of checks where the first can fail or all succeed */
    @ReadPermission(all = { NegativeIntegerUserCheck.class, Role.ALL.class })
    public String field6;

    /* Verifies a chain of checks where the last can fail. */
    @ReadPermission(all = { Role.ALL.class, Role.NONE.class })
    public String field7;

    /* Verifies a chain of checks where all can fail or the last can succeed. */
    @ReadPermission(any = { Role.NONE.class, NegativeIntegerUserCheck.class })
    public String field8;
}
