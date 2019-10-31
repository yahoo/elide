package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

import lombok.Data;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Include(rootLevel = true, type = "table")
@Entity
@Data
public class Table {
    @Id
    private String name;

    private String longName;

    private String description;

    private String category;

    private CardinalitySize cardinality;

    @OneToMany(mappedBy = "table_name")
    private Set<Column> columns;
}
