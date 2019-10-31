package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.metadata.enums.ValueType;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Include(rootLevel = true, type = "dataType")
@Entity
@Data
public class DataType {
    @Id
    private String name;

    private ValueType valueType;
}
