/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Model to represent a Parent's Job.
 *
 * Used to test/demonstrate the use of {@literal @MapsId} annotation.
 */
@Entity
@Table(name = "job")
@Include(name = "job")
public class Job {

    @Id
    @Getter
    private Long jobId;

    @Getter @Setter
    private String title;

    @OneToOne
    @MapsId
    @Getter @Setter
    private Parent parent;
}
