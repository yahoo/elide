/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Contact extends SwaggerComponent {
    private static final String[] REQUIRED = {};
    public String name;
    public String url;
    public String email;
    public Contact()
    {
        required = REQUIRED;
    }
    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        if(url != null && !Util.validateURL(url))
            throw new SwaggerValidationException("The URL must be properly formatted");

        if(email != null && !Util.validateEmail(email))
            throw new SwaggerValidationException("The email address must be properly formatted");
    }
}
