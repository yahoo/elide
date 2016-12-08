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
