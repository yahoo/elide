package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Tag extends SwaggerComponent {
    private static final String[] REQUIRED = {"name"};
    public String name;
    public String description;
    public ExternalDocumentation externalDocs;
    public Tag()
    {
        required = REQUIRED;
    }
}
