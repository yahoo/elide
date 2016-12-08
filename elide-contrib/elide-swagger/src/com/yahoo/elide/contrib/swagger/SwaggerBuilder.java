package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.core.EntityDictionary;

public class SwaggerBuilder {
    public SwaggerBuilder(EntityDictionary entityDictionary)
    {
        Swagger retval = new Swagger();
        // Most of the stuff in this object is for humans to read, which will make 
        // it really hard to automatically generate. We might need some annotations
        // to make this work.
        Info info = new Info();
        retval.info = info;
        // Since the server implementation is separate from Elide (I think), we can't know
        // this. Knowing this means that the user would have to write it twice, once, for 
        // the server and once for us, but I don't know how to get around that.
        // TODO: Find a better way to know this.
        Enums.Scheme[] schemes = new Enum.Scheme[] {Enum.Scheme.HTTP};
        retval.schemes = schemes;

        // If I understand correctly, swagger is set up so that it can only accept and 
        // return json text, so I think we can hardcode this. Maybe?
        retval.consumes = new MimeType[] {new MimeType("application/json")};
        retval.produces = new MimeType[] {new MimeType("application/json")};
    }
}
