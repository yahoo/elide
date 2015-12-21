/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.Access;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Include(rootLevel = true)
@ReadPermission(any = {Access.ALL.class})
@CreatePermission(any = {Access.ALL.class})
@UpdatePermission(any = {Access.ALL.class})
@Entity
public class OneToOneRoot {
    private Long id;

    private String name;

    private OneToOneNonRoot otherObject;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToOne
    public OneToOneNonRoot getOtherObject() {
        return otherObject;
    }

    public void setOtherObject(OneToOneNonRoot otherObject) {
        this.otherObject = otherObject;
    }
}
