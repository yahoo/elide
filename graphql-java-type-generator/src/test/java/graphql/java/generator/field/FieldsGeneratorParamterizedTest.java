package graphql.java.generator.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import graphql.java.generator.BuildContext;
import graphql.java.generator.DefaultBuildContext;
import graphql.java.generator.RecursiveClass;
import graphql.java.generator.BuildContext.Builder;
import graphql.java.generator.argument.ArgumentsGenerator;
import graphql.java.generator.argument.strategies.ArgumentStrategies;
import graphql.java.generator.field.reflect.FieldObjects_Reflection;
import graphql.java.generator.field.reflect.FieldObjects_ReflectionClassFields;
import graphql.java.generator.field.reflect.FieldObjects_ReflectionClassMethods;
import graphql.java.generator.field.strategies.FieldStrategies;
import graphql.java.generator.type.FullTypeGenerator;
import graphql.java.generator.type.TypeGenerator;
import graphql.java.generator.type.WrappingTypeGenerator;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLFieldDefinition;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class FieldsGeneratorParamterizedTest {
    private static Logger logger = LoggerFactory.getLogger(
            FieldsGeneratorParamterizedTest.class);
    
    FieldsGenerator generator;
    BuildContext testContext;
    public FieldsGeneratorParamterizedTest(FieldsGenerator fieldsGen) {
        generator = fieldsGen;
        
        final TypeGenerator defaultTypeGenerator = 
                new WrappingTypeGenerator(new FullTypeGenerator(new TypeStrategies.Builder()
                        .usingTypeRepository(DefaultBuildContext.defaultTypeRepository)
                        .build()));
        
        final ArgumentsGenerator defaultArgumentsGenerator = 
                new ArgumentsGenerator(new ArgumentStrategies.Builder()
                        .build());

        testContext = new Builder()
                .setTypeGeneratorStrategy(defaultTypeGenerator)
                .setFieldsGeneratorStrategy(fieldsGen)
                .setArgumentsGeneratorStrategy(defaultArgumentsGenerator)
                .build();
    }
    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        final FieldsGenerator fieldsByJavaMethods = new FieldsGenerator(
                new FieldStrategies.Builder()
                        .fieldObjectsStrategy(new FieldObjects_ReflectionClassMethods())
                        .build());
        final FieldsGenerator fieldsByJavaFields = new FieldsGenerator(
                new FieldStrategies.Builder()
                        .fieldObjectsStrategy(new FieldObjects_ReflectionClassFields())
                        .build());
        final FieldsGenerator fieldsCombined = new FieldsGenerator(
                new FieldStrategies.Builder()
                        .fieldObjectsStrategy(new FieldObjects_Reflection())
                        .build());
        @SuppressWarnings("serial")
        ArrayList<Object[]> list = new ArrayList<Object[]>() {{
            add(new Object[] {fieldsByJavaMethods});
            add(new Object[] {fieldsByJavaFields});
            add(new Object[] {fieldsCombined});
        }};
        return list;
    }

    @Test
    public void testRecursion() {
        logger.debug("testRecursion");
        Object object = generator.getOutputFields(RecursiveClass.class);
        Assert.assertThat(object, instanceOf(List.class));
        List<GraphQLFieldDefinition> recursiveFields = (List<GraphQLFieldDefinition>) object;
        
        Matcher<Iterable<GraphQLFieldDefinition>> hasItemsMatcher =
                hasItems(
                        hasProperty("name", is("recursionLevel")),
                        hasProperty("name", is("recursive")));
        assertThat(recursiveFields, hasItemsMatcher);
        assertThat(recursiveFields.size(), is(2));
    }
}
