package com.yahoo.elide.contrib.swagger;

public class Info extends SwaggerComponent {
    protected String[] required = {"title", "version"};
    public String title;
    public String description;
    public String termsOfService;
    public Contact contact;
    public License license;
    public String version;
}
