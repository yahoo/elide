package com.yahoo.elide.datastores.aggregation.example;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.VariableFieldSerializer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Include(rootLevel = true)
public class Book {
    @Getter
    @Setter
    @Id
    private long id;

    @Getter
    @Setter
    @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
    private String title;

    @Getter
    @Setter
    @ManyToMany
    private Collection<Author> authors = new ArrayList<>();
}


