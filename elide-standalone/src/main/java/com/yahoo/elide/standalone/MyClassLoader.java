package com.yahoo.elide.standalone;

import org.mdkt.compiler.DynamicClassLoader;

import java.net.MalformedURLException;
import java.net.URL;

public class MyClassLoader extends DynamicClassLoader {
    public MyClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    protected URL findResource(String name) {
        if (name.equals("com/yahoo/elide/standalone/models/Post.class")) {
            try {
                return new URL("file://com/yahoo/elide/standalone/models/Post.class");
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        return super.findResource(name);
    }
}
