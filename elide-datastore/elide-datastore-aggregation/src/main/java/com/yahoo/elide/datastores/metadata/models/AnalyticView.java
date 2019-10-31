package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "analyticView")
@Entity
@Data
public class AnalyticView extends Table {
    private Set<Metric> metrics;

    private Set<Dimension> dimensions;

    private Set<Column> columns;

    @Override
    @Exclude
    @OneToMany(mappedBy = "table_name")
    public Set<Column> getColumns() {
        // Columns are hidden for analytic views
        return columns;
    }
}
