/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.RequestScope;

import javax.persistence.Entity;

/**
 * Bean with only computed fields
 */
@Include
@Entity
public class ComputedBean {

    @ComputedAttribute
    public String getTest() {
        return "test1";
    }

    @ComputedAttribute
    public String getTestWithScope(RequestScope requestScope) {
        return "test2";
    }

    @ComputedAttribute
    public String getTestWithSecurityScope(com.yahoo.elide.security.RequestScope requestScope) {
        return "test3";
    }

    public String getNonComputedWithScope(RequestScope requestScope) {
        return "should not run!";
    }
}
