/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.security.RequestScope;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * Bean with only computed fields
 */
@Include
@Entity
public class ComputedBean {

    private long id;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ComputedAttribute
    @Transient
    public String getTest() {
        return "test1";
    }

    @ComputedAttribute
    @Transient
    public String getTestWithScope(RequestScope requestScope) {
        return "test2";
    }

    @ComputedAttribute
    @Transient
    public String getTestWithSecurityScope(com.yahoo.elide.security.RequestScope requestScope) {
        return "test3";
    }

    public String getNonComputedWithScope(RequestScope requestScope) {
        return "should not run!";
    }
}
