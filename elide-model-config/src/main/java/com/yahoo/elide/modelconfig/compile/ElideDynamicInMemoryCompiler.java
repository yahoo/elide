/*
 * Copyright 2020, the original author or authors.
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.DynamicClassLoader;
import org.mdkt.compiler.SourceCode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Elide specific version of InMemoryJavaCompiler using custom JavaFileManager.
 * This class is based on:
 * https://github.com/trung/InMemoryJavaCompiler/blob/master/src/main/java/org/mdkt/compiler/InMemoryJavaCompiler.java
 */
public class ElideDynamicInMemoryCompiler {
    private JavaCompiler javac;
    private DynamicClassLoader classLoader;
    private Iterable<String> options;
    boolean ignoreWarnings = false;

    private Map<String, SourceCode> sourceCodes = new HashMap<String, SourceCode>();

    public static ElideDynamicInMemoryCompiler newInstance() {
        return new ElideDynamicInMemoryCompiler();
    }

    private ElideDynamicInMemoryCompiler() {
        this.javac = ToolProvider.getSystemJavaCompiler();
        this.classLoader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
    }

    public ElideDynamicInMemoryCompiler useParentClassLoader(ClassLoader parent) {
        this.classLoader = new DynamicClassLoader(parent);
        return this;
    }

    /**
     * Get ClassLoader.
     *
     * @return the class loader used internally by the compiler
     */
    public ClassLoader getClassloader() {
        return classLoader;
    }

    /**
     * Options used by the compiler, e.g. '-Xlint:unchecked'.
     *
     * @param options
     * @return ElideDynamicInMemoryCompiler Instance
     */
    public ElideDynamicInMemoryCompiler useOptions(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    /**
     * Ignore non-critical compiler output, like unchecked/unsafe operation
     * warnings.
     *
     * @return ElideDynamicInMemoryCompiler Instance
     */
    public ElideDynamicInMemoryCompiler ignoreWarnings() {
        ignoreWarnings = true;
        return this;
    }

    /**
     * Compile all sources.
     *
     * @return Map containing instances of all compiled classes
     * @throws Exception Exception Thrown
     */
    public Map<String, Class<?>> compileAll() throws Exception {
        if (sourceCodes.size() == 0) {
            throw new CompilationException("No source code to compile");
        }
        Collection<SourceCode> compilationUnits = sourceCodes.values();
        CompiledCode[] code;

        code = new CompiledCode[compilationUnits.size()];
        Iterator<SourceCode> iter = compilationUnits.iterator();
        for (int i = 0; i < code.length; i++) {
            code[i] = new CompiledCode(iter.next().getClassName());
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        JavaFileManager fileManager = new ElideDynamicJavaFileManager(classLoader,
                javac.getStandardFileManager(null, null, null));
        JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, collector, options, null,
                compilationUnits);
        boolean result = task.call();
        if (!result || collector.getDiagnostics().size() > 0) {
            StringBuffer exceptionMsg = new StringBuffer();
            exceptionMsg.append("Unable to compile the source");
            boolean hasWarnings = false;
            boolean hasErrors = false;
            for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
                switch (d.getKind()) {
                case NOTE:
                case MANDATORY_WARNING:
                case WARNING:
                    hasWarnings = true;
                    break;
                case OTHER:
                case ERROR:
                default:
                    hasErrors = true;
                    break;
                }
                exceptionMsg.append("\n").append("[kind=").append(d.getKind());
                exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
                exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
            }
            if (hasWarnings && !ignoreWarnings || hasErrors) {
                throw new CompilationException(exceptionMsg.toString());
            }
        }

        Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
        for (String className : sourceCodes.keySet()) {
            classes.put(className, classLoader.loadClass(className));
        }
        return classes;
    }

    /**
     * Compile single source.
     *
     * @param className
     * @param sourceCode
     * @return Class
     * @throws Exception
     */
    public Class<?> compile(String className, String sourceCode) throws Exception {
        return addSource(className, sourceCode).compileAll().get(className);
    }

    /**
     * Add source code to the compiler.
     *
     * @param className
     * @param sourceCode
     * @return ElideDynamicInMemoryCompiler Instance
     * @throws Exception
     * @see #compileAll()
     */
    public ElideDynamicInMemoryCompiler addSource(String className, String sourceCode) throws Exception {
        sourceCodes.put(className, new SourceCode(className, sourceCode));
        return this;
    }
}
