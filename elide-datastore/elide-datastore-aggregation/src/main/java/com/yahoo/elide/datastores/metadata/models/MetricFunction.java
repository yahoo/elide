package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;

import lombok.Data;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Include(rootLevel = true, type = "metricFunction")
@Entity
@Data
public class MetricFunction {
    @Id
    private String name;

    private String longName;

    private String description;

    @ManyToMany()
    private Set<FunctionArgument> arguments;
}
