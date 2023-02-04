/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bean with Map of Enums.
 */
@Entity
@Table(name = "color_shape")
@Include
public class MapColorShape extends BaseId {
    private Map<Color, Shape> colorShapeMap = new LinkedHashMap<>();

    @ElementCollection
    @MapKeyColumn(name = "color")
    @Column(name = "shape")
    public Map<Color, Shape> getColorShapeMap() {
        return colorShapeMap;
    }

    public void setColorShapeMap(Map<Color, Shape> colorShapeMap) {
        this.colorShapeMap = colorShapeMap;
    }
}
