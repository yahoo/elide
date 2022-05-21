/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.targetEntity;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Include(name = "swe")
@Data
public class SWE {
    @Id
    @GeneratedValue
    private long id;

    private String name;

    @OneToOne(targetEntity = Manager.class)
    private Employee boss;
}
