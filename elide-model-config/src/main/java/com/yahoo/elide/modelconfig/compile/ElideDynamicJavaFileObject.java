/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.tools.SimpleJavaFileObject;

/**
 * Elide specific implementation of SimpleJavaFileObject for Jar support.
 */
public class ElideDynamicJavaFileObject extends SimpleJavaFileObject {
    private static final String JAR = "jar";
    private static final String COLON = ":";

    private final String qualifiedClassName;
    private URI uri; //SimpleJavaObject.uri errors if URI begins with 'jar:'.

    public ElideDynamicJavaFileObject(String qualifiedClassName, URI uri) {
        super(uri, Kind.CLASS);
        this.uri = uri;
        this.qualifiedClassName = qualifiedClassName;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        if (uri.toString().contains(JAR)) {
            try {
                uri = new URI(JAR + COLON + uri.toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return uri.toURL().openStream();
    }

    @Override
    public String getName() {
        return uri.getPath() == null ? uri.getSchemeSpecificPart() : uri.getPath();
    }

    /**
     * Infers a binary name of the JavaFileObject.
     * @return Fully Qualified Class Name
     */
    public String inferredBinaryName() {
        return qualifiedClassName;
    }
}
