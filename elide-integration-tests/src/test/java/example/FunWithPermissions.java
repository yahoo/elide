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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;

/**
 * Permission checks test bean.
 */
@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.None OR Prefab.Role.All")
@DeletePermission(expression = "Prefab.Role.None AND Prefab.Role.All")
@Include(name = "fun") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "fun")
public class FunWithPermissions extends BaseId {
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
    @OneToOne (cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    public Child getRelation3() {
        return relation3;
    }

    public void setRelation3(Child relation3) {
        this.relation3 = relation3;
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
