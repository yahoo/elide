/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bean with Map of Enums.
 */
@Entity
@Table(name = "color_shape")
@Include
public class MapColorShape {
    private long id;
    private String name;
    private Map<Color, Shape> colorShapeMap = new LinkedHashMap<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Color, Shape> getColorShapeMap() {
        return colorShapeMap;
    }

    public void setColorShapeMap(Map<Color, Shape> colorShapeMap) {
        this.colorShapeMap = colorShapeMap;
    }
}
