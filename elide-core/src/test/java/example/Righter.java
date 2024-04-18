/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.UpdatePermission;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;


@Include(name = "righter")
@UpdatePermission(expression = "Prefab.Role.None")
@Entity
@DiscriminatorValue("righter")
public class Righter extends Right {
    private String moreRight;

    public String getMoreRight() {
        return moreRight;
    }

    public void setMoreRight(String moreRight) {
        this.moreRight = moreRight;
    }
}
