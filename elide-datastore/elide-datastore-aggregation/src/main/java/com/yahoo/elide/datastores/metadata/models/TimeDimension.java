package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "analyticView")
@Entity
@Data
public class TimeDimension extends Dimension {
    Set<TimeGrain> supportedGrains;
}
