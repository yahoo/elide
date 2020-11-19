/*
 * Copyright 2020, the original author or authors.
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mdkt.compiler.CompilationException;

import java.util.List;
import java.util.Map;

/**
 * This class is based on.
 * https://github.com/trung/InMemoryJavaCompiler/blob/master/src/test/java/org/mdkt/compiler/InMemoryJavaCompilerTest.java
 */
public class ElideDynamicInMemoryCompilerTest {

    @Test
    public void compileWhenTypical() throws Exception {
        StringBuffer sourceCode = new StringBuffer();

        sourceCode.append("package org.mdkt;\n");
        sourceCode.append("public class HelloClass {\n");
        sourceCode.append("   public String hello() { return \"hello\"; }");
        sourceCode.append("}");

        Class<?> helloClass = ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings().compile("org.mdkt.HelloClass", sourceCode.toString());
        assertNotNull(helloClass);
        assertEquals(1, helloClass.getDeclaredMethods().length);
    }

    @Test
    public void compileAllWhenTypical() throws Exception {
        String cls1 = "public class A{ public B b() { return new B(); }}";
        String cls2 = "public class B{ public String toString() { return \"B!\"; }}";

        Map<String, Class<?>> compiled = ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings().addSource("A", cls1).addSource("B", cls2).compileAll();

        assertNotNull(compiled.get("A"));
        assertNotNull(compiled.get("B"));

        Class<?> aClass = compiled.get("A");
        Object a = aClass.newInstance();
        assertEquals("B!", aClass.getMethod("b").invoke(a).toString());
    }

    @Test
    public void compileWhenSourceContainsInnerClasses() throws Exception {
        StringBuffer sourceCode = new StringBuffer();

        sourceCode.append("package org.mdkt;\n");
        sourceCode.append("public class HelloClass {\n");
        sourceCode.append("   private static class InnerHelloWorld { int inner; }\n");
        sourceCode.append("   public String hello() { return \"hello\"; }");
        sourceCode.append("}");

        Class<?> helloClass = ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings().compile("org.mdkt.HelloClass", sourceCode.toString());
        assertNotNull(helloClass);
        assertEquals(1, helloClass.getDeclaredMethods().length);
    }

    @Test
    public void compileWhenError() throws Exception {
        Exception e = assertThrows(CompilationException.class, () -> {
                StringBuffer sourceCode = new StringBuffer();

                sourceCode.append("package org.mdkt;\n");
                sourceCode.append("public classHelloClass {\n");
                sourceCode.append("   public String hello() { return \"hello\"; }");
                sourceCode.append("}");
                ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings().compile("org.mdkt.HelloClass", sourceCode.toString());
            }
        );
        assertTrue(e.getMessage().contains("Unable to compile the source"));
    }

    @Test
    public void compileWhenFailOnWarnings() throws Exception {
        assertThrows(CompilationException.class, () -> {
                StringBuffer sourceCode = new StringBuffer();

                sourceCode.append("package org.mdkt;\n");
                sourceCode.append("public class HelloClass {\n");
                sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
                sourceCode.append("}");
                ElideDynamicInMemoryCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
            }
        );
    }

    @Test
    public void compileWhenIgnoreWarnings() throws Exception {
        StringBuffer sourceCode = new StringBuffer();

        sourceCode.append("package org.mdkt;\n");
        sourceCode.append("public class HelloClass {\n");
        sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
        sourceCode.append("}");
        Class<?> helloClass = ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings().compile("org.mdkt.HelloClass", sourceCode.toString());
        List<?> res = (List<?>) helloClass.getMethod("hello").invoke(helloClass.newInstance());
        assertEquals(0, res.size());
    }

    @Test
    public void compileWhenWarningsAndErrors() throws Exception {
        assertThrows(CompilationException.class, () -> {
                StringBuffer sourceCode = new StringBuffer();

                sourceCode.append("package org.mdkt;\n");
                sourceCode.append("public class HelloClass extends xxx {\n");
                sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
                sourceCode.append("}");

                ElideDynamicInMemoryCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
            }
        );
    }
}
