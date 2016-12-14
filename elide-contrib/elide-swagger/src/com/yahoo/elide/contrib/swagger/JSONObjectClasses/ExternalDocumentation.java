/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class ExternalDocumentation extends SwaggerComponent {
    private static final String[] REQUIRED = {"url"};
    public String description;
    public String url;
    public ExternalDocumentation()
    {
        required = REQUIRED;
    }
}
