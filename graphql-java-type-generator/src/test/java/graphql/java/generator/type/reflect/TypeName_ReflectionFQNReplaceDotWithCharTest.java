package graphql.java.generator.type.reflect;

import graphql.java.generator.ClassWithLists;
import graphql.java.generator.type.strategies.TypeNameStrategy;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class TypeName_ReflectionFQNReplaceDotWithCharTest {
    @Test
    public void testObjects() {
        TypeNameStrategy strategy = new TypeName_ReflectionFQNReplaceDotWithChar();
        String typeName = strategy.getTypeName(new Object());
        assertThat(typeName, is("java_lang_Object"));
        typeName = strategy.getTypeName(new ClassWithLists());
        assertThat(typeName, is("graphql_java_generator_ClassWithLists"));
    }
    
    @Test
    public void testClasses() throws ClassNotFoundException {
        TypeNameStrategy strategy = new TypeName_ReflectionFQNReplaceDotWithChar();
        String typeName = strategy.getTypeName(Object.class);
        assertThat(typeName, is("java_lang_Object"));
        typeName = strategy.getTypeName(ClassWithLists.class);
        assertThat(typeName, is("graphql_java_generator_ClassWithLists"));
        typeName = strategy.getTypeName(Class.forName("DefaultPackageClass"));
        assertThat(typeName, is("DefaultPackageClass"));
    }
}
