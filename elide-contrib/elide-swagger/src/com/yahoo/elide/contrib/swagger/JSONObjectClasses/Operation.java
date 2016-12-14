/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

import java.util.HashSet;
import java.lang.IllegalArgumentException;
import java.util.Arrays;

public class Operation extends SwaggerComponent {
    private static HashSet<String> usedOperationIds = new HashSet<>();
    private static final String[] REQUIRED = {"responses"};

    public String[] tags;
    public String summary;
    public String description;
    public ExternalDocumentation externalDocs;
    private String operationId;
    public MimeType[] consumes;
    public MimeType[] produces;
    public Parameter[] parameters;
    public Responses responses;
    public Enums.Scheme[] schemes;
    public boolean deprecated = false;
    public SecurityRequirement[] security;

    public Operation()
    {
        required = REQUIRED;
    }
    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();

        if(Util.hasDuplicates(parameters))
            throw new SwaggerValidationException("Parameters can't have duplicates in it");
        boolean foundBody = false;
        for(int i = 0; i < parameters.length; i++)
        {
            if(parameters[i].in == Enums.Location.BODY)
                if(foundBody)
                    throw new SwaggerValidationException("You can't have more than one parameter in the body");
                else
                    foundBody = true;
        }

        // I'm not 100% sure if this is right. I wrote a very specific comment calling for it, 
        // but it doesn't really make sense to me, and I can't find it again in the spec. 
outer: 
        for(Parameter p : parameters)
        {
            if(p.type == Enums.Type.FILE)
            {
                if(consumes != null)
                {
                    for(MimeType type : consumes)
                    {
                        if(type.toString().equals("multipart/form-data") || type.toString().equals("application/x-www-form-urlencoded"))
                            break outer;
                    }
                }
                throw new SwaggerValidationException("According to the spec, if the type of a parameter is a file, then you have to have either application/x-www-form-urlencoded or multipart/form-data as a mime type that can be consumed");
            }
        }
    }

    public void setOperationId(String id) throws IllegalArgumentException
    {
        if(id.equals(operationId) || !usedOperationIds.contains(id))
        {
            if(operationId != null)
                usedOperationIds.remove(operationId);
            usedOperationIds.add(id);
            operationId = id;
        }
        else
        {
            throw new IllegalArgumentException("You can't recycle an operationId!");
        }
    }
    public String getOperationId()
    {
        return operationId;
    }
}
