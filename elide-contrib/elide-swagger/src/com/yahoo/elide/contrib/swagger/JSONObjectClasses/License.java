/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class License extends SwaggerComponent {
    private static final String[] REQUIRED = {"name"};
    public String name;
    public String url;
    public License()
    {
        required = REQUIRED;
    }
}
