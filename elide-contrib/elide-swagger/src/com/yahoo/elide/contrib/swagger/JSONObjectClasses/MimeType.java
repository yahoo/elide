/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

/*
 * This class is just here to prevent string typing.
 * I wanted things to be more self-documenting, and the 
 * name of the things that hold these don't do a good job of telling you what they are. 
 */
public class MimeType {
    private String mimeType;
    public MimeType(String type)
    {
        this.mimeType = type;
    }
    public String toString()
    {
        return this.mimeType;
    }
}
