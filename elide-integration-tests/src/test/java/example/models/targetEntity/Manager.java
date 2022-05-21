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
import javax.persistence.OneToMany;
import java.util.Set;

@Include(name = "boss")
@Data
public class Manager implements Employee {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @OneToMany(targetEntity = SWE.class, mappedBy = "boss")
    Set<Employee> reports;
}
