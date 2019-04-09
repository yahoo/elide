/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Model for ghostwriters.
 */
@Entity
@Table(name = "job")
@Include(rootLevel = true, type = "job")
public class Job extends BaseId {

    @OneToOne
    @MapsId
    @Getter @Setter
    private Parent parent;

    @Getter @Setter
    private String title;
}
