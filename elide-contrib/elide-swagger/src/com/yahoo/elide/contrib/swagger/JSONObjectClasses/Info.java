/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Info extends SwaggerComponent {
    private static final String[] REQUIRED = {"title", "version"};
    public String title;
    public String description;
    public String termsOfService;
    public Contact contact;
    public License license;
    public String version;
    public Info()
    {
        this.required = REQUIRED;
    }
}
