/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.targetEntity;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import java.util.Set;
import javax.persistence.*;

@Include(name = "boss")
@Data
@Entity(name = "boss")
public class Manager implements Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @OneToMany(targetEntity = SWE.class, mappedBy = "boss")
    Set<Employee> reports;
}
