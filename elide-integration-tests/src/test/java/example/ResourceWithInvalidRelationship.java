/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Include(rootLevel = true)
@ReadPermission(expression = "allow all")
@CreatePermission(expression = "allow all")
@UpdatePermission(expression = "allow all")
@Entity
public class ResourceWithInvalidRelationship extends BaseId {
    private String name;

    private NotIncludedResource notIncludedResource;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToOne
    public NotIncludedResource getNotIncludedResource() {
        return notIncludedResource;
    }

    public void setNotIncludedResource(NotIncludedResource notIncludedResource) {
        this.notIncludedResource = notIncludedResource;
    }
}
