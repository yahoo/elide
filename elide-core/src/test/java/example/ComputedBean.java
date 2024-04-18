/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.ComputedAttribute;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.RequestScope;

import jakarta.persistence.Entity;

/**
 * Bean with only computed fields.
 */
@Include(rootLevel = false)
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
    public String getTestWithSecurityScope(com.paiondata.elide.core.security.RequestScope requestScope) {
        return "test3";
    }

    public String getNonComputedWithScope(RequestScope requestScope) {
        return "should not run!";
    }
}
