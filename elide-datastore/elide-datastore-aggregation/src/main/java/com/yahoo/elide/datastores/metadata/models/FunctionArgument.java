package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Include(rootLevel = true, type = "functionArgument")
@Entity
@Data
public class FunctionArgument {
    @Id
    private String name;

    private String description;

    private DataType valueType;
}
