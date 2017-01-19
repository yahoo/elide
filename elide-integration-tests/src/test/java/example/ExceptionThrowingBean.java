/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Include(rootLevel = true)
@Entity
public class ExceptionThrowingBean {
    private long id;

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Transient
    @ComputedAttribute
    public String getBadValue() {
        throw new WebApplicationException("This should bubble up to the top!", Response.Status.GONE);
    }

    public void setBadValue(String unused) {
        // Do nothing
    }
}
