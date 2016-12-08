package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

import java.util.HashSet;
import java.util.Set;

// https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md

public class Swagger extends SwaggerComponent {
    protected static Swagger main;
    private static final String[] REQUIRED = {"swagger", "info", "paths"};
    // This is specified to be this value. 
    // TODO: Should I somehow externalize these values so that
    // they are easier to increment?
    public String swagger = "2.0";
    public Info info;
    public String host;
    public String basePath = "/";
    public Enums.Scheme[] schemes;
    public MimeType[] consumes;
    public MimeType[] produces;
    public Paths paths;
    public Definitions definitions;
    // I haven't the slightest idea how we're to implement this. I think one could make the case that 
    // this is for the benefit of whatever poor soul has to write this by hand, but the computer doesn't 
    // care, so we could just leave it out.
    public ParametersDefinitions parameters;

    public ResponsesDefinitions responses;
    public SecurityDefinitions securityDefinitions;
    public SecurityRequirement[] security;
    public Tag[] tags;
    public ExternalDocumentation externalDocs;
    public Swagger()
    {
        main = this;
        this.required = REQUIRED;
    }

    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();
        if(!swagger.equals("2.0"))
            throw new SwaggerValidationException("The swagger version must be 2.0");
        if(basePath != null && basePath.charAt(0) != '/')
            throw new SwaggerValidationException("The first letter of the basePath must be /");
        if(tags != null && Util.hasDuplicates(tags))
            throw new SwaggerValidationException("Tags can't have duplicates in it");
    }
}
