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
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Model to represent a Parent's Job.
 *
 * Used to test/demonstrate the use of {@literal @MapsId} annotation.
 */
@Entity
@Table(name = "job")
@Include(rootLevel = true, type = "job")
public class Job {

    @Id
    @Getter
    private Long id;

    @Getter @Setter
    private String title;

    @OneToOne
    @MapsId
    @Getter @Setter
    private Parent parent;
}
