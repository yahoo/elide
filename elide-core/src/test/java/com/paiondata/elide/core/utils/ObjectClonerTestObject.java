/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import java.util.List;

/**
 * Test object for ObjectCloner.
 */
public class ObjectClonerTestObject {
    private Long id;
    private boolean admin;
    private String name;
    private List<String> list;

    public void setId(Long id) {
        this.id = id;
    }
    public boolean isAdmin() {
        return admin;
    }
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<String> getList() {
        return list;
    }
    public void setList(List<String> list) {
        this.list = list;
    }

    public Long id() {
        return this.id;
    }
}
