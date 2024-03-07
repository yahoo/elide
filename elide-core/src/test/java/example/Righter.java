/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Set;


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
