package com.yahoo.elide.contrib.swagger;

import java.util.HashMap;
import java.lang.IllegalArgumentException;

public class Paths extends HashMap<String, Path> {

    @Override
    public Path put(String k, Path v)
    {
        if(!k.startsWith("/"))
            throw new IllegalArgumentException("Paths must start with a slash");
        super.put(k, v);

        return v;
    }
}
