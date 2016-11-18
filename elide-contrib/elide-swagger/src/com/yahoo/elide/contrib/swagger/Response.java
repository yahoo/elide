package com.yahoo.elide.contrib.swagger;

public class Response extends SwaggerComponent {
    private static final String[] REQUIRED = {"description"};
    public String description;
    public Schema schema;
    public Headers headers;
    public Example examples;

    public Response()
    {
        required = REQUIRED;
    }
}
