/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.embeddedid;

import com.yahoo.elide.annotation.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Include
@Data
@Entity
public class Building {
    @EmbeddedId
    @Embedded
    private Address address;

    private String name;

    @OneToMany
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Building> neighbors;
}
