package com.yahoo.elide.contrib.swagger;

public class Contact extends SwaggerComponent {
    private static final String[] REQUIRED = {};
    public String name;
    public String url;
    public String email;
    public Contact()
    {
        required = REQUIRED;
    }
}
