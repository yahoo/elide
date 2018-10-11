/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.ToOne;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.MetaValue;

import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

@Entity
@Include(rootLevel = true, type = "property")
@SharePermission
public class Property extends BaseId {
    @Setter private Device myStuff;

    @Any(metaColumn = @Column(name = "property_type"))
    @AnyMetaDef(idType = "long", metaType = "string", metaValues = {
            @MetaValue(targetEntity = example.Tractor.class, value = "tractor"),
            @MetaValue(targetEntity = example.Smartphone.class, value = "smartphone")
    })
    @JoinColumn(name = "property_id")
    @ToOne
    public Device getMyStuff() {
        return myStuff;
    }
}
