/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include(rootLevel = true)
@Audited // Ensure envers does not cause any issues
public class Person {
    @Setter
    private long id;

    @Setter
    @Getter
    private String name;

    @Setter
    private AddressFragment address;

    @Id
    public long getId() {
        return id;
    }

    @Column(name = "address", columnDefinition = "TEXT")
    @Convert(converter = AddressFragment.Converter.class)
    public AddressFragment getAddress() {
        return address;
    }
}
