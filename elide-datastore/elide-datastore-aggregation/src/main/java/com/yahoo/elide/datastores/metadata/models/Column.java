package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.metadata.enums.Tag;

import lombok.Data;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Include(rootLevel = true, type = "column")
@Entity
@Data
public class Column {
    @Id
    private String id;

    private String name;

    private String longName;

    private String description;

    private String category;

    @ManyToMany
    private DataType dataType;

    private Set<Tag> columnTags;
}
