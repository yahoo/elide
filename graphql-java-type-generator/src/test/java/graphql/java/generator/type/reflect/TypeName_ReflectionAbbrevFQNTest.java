package graphql.java.generator.type.reflect;

import graphql.java.generator.ClassWithLists;
import graphql.java.generator.type.strategies.TypeNameStrategy;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class TypeName_ReflectionAbbrevFQNTest {
    @Test
    public void testObjects() {
        TypeNameStrategy strategy = new TypeName_ReflectionAbbrevFQN();
        String typeName = strategy.getTypeName(new Object());
        assertThat(typeName, is("jlObject"));
        typeName = strategy.getTypeName(new ClassWithLists());
        assertThat(typeName, is("gjgClassWithLists"));
    }
    
    @Test
    public void testClasses() throws ClassNotFoundException {
        TypeNameStrategy strategy = new TypeName_ReflectionAbbrevFQN();
        String typeName = strategy.getTypeName(Object.class);
        assertThat(typeName, is("jlObject"));
        typeName = strategy.getTypeName(ClassWithLists.class);
        assertThat(typeName, is("gjgClassWithLists"));
        typeName = strategy.getTypeName(Class.forName("DefaultPackageClass"));
        assertThat(typeName, is("DefaultPackageClass"));
    }
}
