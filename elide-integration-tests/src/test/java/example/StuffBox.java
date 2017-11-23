/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import com.yahoo.elide.annotation.ToMany;
import lombok.Setter;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import java.util.Collection;

@Entity
@Include(rootLevel = true, type = "stuffbox")
@SharePermission(expression = "allow all")
public class StuffBox extends BaseId {
    @Setter private Collection<Device> myStuff;

    @ManyToAny(metaColumn = @Column(name = "property_type"))
    @AnyMetaDef(idType = "long", metaType = "string", metaValues = {
            @MetaValue(targetEntity = example.Tractor.class, value = "tractor"),
            @MetaValue(targetEntity = example.Smartphone.class, value = "smartphone")
    })
    @JoinTable(
            name = "property_in_box",
            joinColumns = @JoinColumn(name = "stuff_id"),
            inverseJoinColumns = @JoinColumn(name = "property_id")
    )
    @ToMany
    public Collection<Device> getMyStuff() {
        return myStuff;
    }
}
