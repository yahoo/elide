package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class SecurityScheme extends SwaggerComponent {
    private static final String[] REQUIRED = {"type", "name", "in", "flow", "authorizationUrl", "tokenUrl", "scopes"};

    public Enums.SecurityType type;
    public String description;
    public String name;
    public Enums.ApiKeyLocation in;
    public Enums.Flow flow;
    public String authorizationUrl;
    public String tokenUrl;
    public Scopes scopes;


    public SecurityScheme()
    {
        required = REQUIRED;
    }
}
