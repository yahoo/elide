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
    @Override
    public void checkRequired()
    {
        if(!Util.validateURL(url))
            throw new RuntimeException("The URL must be properly formatted");

        if(!Util.validateEmail(email))
            throw new RuntimeException("The email address must be properly formatted");
    }
}
