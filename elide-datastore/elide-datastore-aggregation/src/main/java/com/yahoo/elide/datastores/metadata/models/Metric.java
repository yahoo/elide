package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.metadata.enums.Format;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "metric")
@Entity
@Data
public class Metric extends Column {
    private Format defaultFormat; //Decimal Precision

    @ManyToMany
    private Set<MetricFunction> supportedFunctions;
}
