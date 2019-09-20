package com.yahoo.elide.datastores.aggregation.example;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.VariableFieldSerializer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include(rootLevel = true)
public class Author {
    @Getter
    @Setter
    @Id
    private Long id;

    @Getter
    @Setter
    @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
    private String name;
}
