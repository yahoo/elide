/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

import java.util.HashMap;
import java.lang.IllegalArgumentException;

public class Paths extends HashMap<String, Path> {

    @Override
    public Path put(String k, Path v)
    {
        if(!k.startsWith("/"))
            throw new IllegalArgumentException("Paths must start with a slash");
        super.put(k, v);

        return v;
    }
}
