/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@Include(rootLevel = true, type = "smartphone")
@SharePermission
public class Smartphone extends BaseId implements Device {
    @Getter @Setter private String type;

    @ComputedAttribute
    @Transient
    public String getOperatingSystem() {
        String os;
        switch (type) {
            case "android":
                os = "some dessert";
                break;
            case "iphone":
                os = "iOS";
                break;
            default:
                os = "who cares?";
                break;
        }

        return os;
    }
}
