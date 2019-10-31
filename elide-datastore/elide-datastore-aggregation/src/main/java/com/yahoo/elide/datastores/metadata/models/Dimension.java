package com.yahoo.elide.datastores.metadata.models;

import com.yahoo.elide.annotation.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "dimension")
@Entity
@Data
public class Dimension extends Column {
}
