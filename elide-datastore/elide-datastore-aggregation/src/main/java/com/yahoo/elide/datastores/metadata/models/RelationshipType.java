package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@Entity
@Include(rootLevel = true, type = "relationshipType")
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationshipType extends DataType {
    private String tableName;
}
