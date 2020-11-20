/*
 * Copyright 2020, the original author or authors.
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import org.apache.commons.io.FilenameUtils;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.DynamicClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Elide specific implementation of JavaFileManager.
 * This class is based on:
 * https://github.com/trung/InMemoryJavaCompiler/blob/master/src/main/java/org/mdkt/compiler/
 * ExtendedStandardJavaFileManager.java
 */
public class ElideDynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private static final String CLASS_EXTENSION = ".class";
    private static final String JAVA_PACKAGE_NAME = "java.";
    private static final String EXCLAMATION_MARK = "!";
    private static final String FORWARD_SLASH = "/";
    private static final String DOT_REGEX = "\\.";
    private static final String JAR = "jar";
    private static final String COLON = ":";

    private List<CompiledCode> compiledCode = new ArrayList<CompiledCode>();
    private DynamicClassLoader cl;

    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager.
     * @param cl DynamicClassLoader instance.
     */
    public ElideDynamicJavaFileManager(DynamicClassLoader cl, JavaFileManager fileManager) {
        super(fileManager);
        this.cl = cl;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return file instanceof ElideDynamicJavaFileObject ? ((ElideDynamicJavaFileObject) file).inferredBinaryName()
                : fileManager.inferBinaryName(location, file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
            JavaFileObject.Kind kind, FileObject sibling) throws IOException {

        try {
            CompiledCode innerClass = new CompiledCode(className);
            compiledCode.add(innerClass);
            cl.addCode(innerClass);
            return innerClass;
        } catch (Exception e) {
            throw new RuntimeException("Error while creating in-memory output file for " + className, e);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
            boolean recurse) throws IOException {

        if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)
                && !packageName.startsWith(JAVA_PACKAGE_NAME)) {
            return search(packageName);
        }

        return fileManager.list(location, packageName, kinds, recurse);
    }


    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return cl;
    }

    private List<JavaFileObject> searchClasses(String packageName, File directory) {
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();

        File[] files = directory.listFiles((dir, name) -> name.endsWith(CLASS_EXTENSION));

        for (File file : files) {
            String className = packageName + "." + FilenameUtils.removeExtension(file.getName());
            result.add(new ElideDynamicJavaFileObject(className, file.toURI()));
        }

        return result;
    }

    private List<JavaFileObject> search(String packageName) throws IOException {
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();

        Collections.list(cl.getResources(packageName.replaceAll(DOT_REGEX, FORWARD_SLASH))).forEach(
                packageNameFolderURL -> {
                    File fileInstance = new File(packageNameFolderURL.getFile());
                    boolean isDir = fileInstance.isDirectory();
                    result.addAll(isDir ? searchClasses(packageName, fileInstance)
                            : searchJarClasses(packageNameFolderURL));
                }
                );

        return result;
    }

    private List<JavaFileObject> searchJarClasses(URL packageNameFolderURL) {
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        try {
            JarURLConnection jarConn = (JarURLConnection) packageNameFolderURL.openConnection();
            String topLevelEntryName = jarConn.getEntryName();

            // ElideDynamicJavaFileObject handles jar file as special case.
            String jarUriString = jarConn.getJarFileURL().getProtocol().equals(JAR)
                    ? jarConn.getJarFileURL().getFile()
                    : jarConn.getJarFileURL().getProtocol() + COLON + jarConn.getJarFileURL().getFile();

            Collections.list(jarConn.getJarFile().entries()).forEach(
                    jarEntry -> {
                        String jarEntryName = jarEntry.getName();

                        if (jarEntryName.startsWith(topLevelEntryName)
                                && jarEntryName.endsWith(CLASS_EXTENSION)
                                && jarEntryName.lastIndexOf(FORWARD_SLASH.charAt(0)) <= topLevelEntryName.length()) {

                            URI classUri = URI.create(jarUriString + EXCLAMATION_MARK + FORWARD_SLASH + jarEntryName);
                            String className = FilenameUtils.removeExtension(jarEntryName)
                                    .replaceAll(FORWARD_SLASH, DOT_REGEX);

                            result.add(new ElideDynamicJavaFileObject(className, classUri));
                        }
                    }
                    );
        } catch (IOException e) {
            throw new RuntimeException("IOException while accessing " + packageNameFolderURL, e);
        }
        return result;
    }
}
