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
