package com.yahoo.elide.contrib.swagger;

import java.util.HashMap;
import java.lang.IllegalArgumentException;

public class Paths extends HashMap<String, Path> {
    @Override
    public PathItem put(String k, PathItem v)
    {
        if(!k.startsWith("/"))
            throw IllegalArgumentException("Paths must start with a slash");
        super.put(k, v);
    }
}
