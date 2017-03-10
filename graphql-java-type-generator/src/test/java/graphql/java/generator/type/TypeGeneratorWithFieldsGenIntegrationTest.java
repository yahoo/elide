package graphql.java.generator.type;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.java.generator.BuildContext;
import graphql.java.generator.ClassWithListOfList;
import graphql.java.generator.ClassWithLists;
import graphql.java.generator.ClassWithRawArrays;
import graphql.java.generator.DefaultBuildContext;
import graphql.java.generator.RecursiveClass;
import graphql.java.generator.BuildContext.Builder;
import graphql.java.generator.argument.ArgumentsGenerator;
import graphql.java.generator.argument.strategies.ArgumentStrategies;
import graphql.java.generator.field.FieldsGenerator;
import graphql.java.generator.field.FieldsGeneratorParamterizedTest;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class TypeGeneratorWithFieldsGenIntegrationTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorWithFieldsGenIntegrationTest.class);
    
    BuildContext testContext;
    
    public TypeGeneratorWithFieldsGenIntegrationTest(FieldsGenerator fieldsGen) {
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
        return FieldsGeneratorParamterizedTest.data();
    }
    
    @Test
    public void testEnum() {
        logger.debug("testEnum");
        Object enumObj = testContext.getOutputType(graphql.java.generator.Enum.class);
        Assert.assertThat(enumObj, instanceOf(GraphQLEnumType.class));
        Matcher<Iterable<GraphQLEnumValueDefinition>> hasItemsMatcher =
                hasItems(
                        hasProperty("name", is("A")),
                        hasProperty("name", is("B")),
                        hasProperty("name", is("C")));
        assertThat(((GraphQLEnumType)enumObj).getValues(), hasItemsMatcher);
        
        enumObj = testContext.getOutputType(graphql.java.generator.EmptyEnum.class);
        Assert.assertThat(enumObj, instanceOf(GraphQLEnumType.class));
        assertThat(((GraphQLEnumType)enumObj).getValues(),
                instanceOf(List.class));
        assertThat(((GraphQLEnumType)enumObj).getValues().size(),
                is(0));
    }
    
    @Ignore("This test current fails, but I believe it to be because"
            + " of a problem in the library, and will construct"
            + " a suitable test case")
    @Test
    public void testRecursion() {
        logger.debug("testRecursion");
        Object recursiveClass = testContext.getOutputType(RecursiveClass.class);
        Assert.assertThat(recursiveClass, instanceOf(GraphQLOutputType.class));
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) recursiveClass)
                        .name("testObj")
                        .staticValue(new RecursiveClass(2))
                        .build())
                .build();
        GraphQLSchema recursiveTestSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    recursionLevel"
        + "    recursive {"
        + "      recursionLevel"
        + "    recursive {"
        + "      recursionLevel"
        + "    recursive {"
        + "      recursionLevel"
        + "    recursive {"
        + "      recursionLevel"
        + "    }"
        + "    }"
        + "    }"
        + "    }"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(recursiveTestSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testRecursion results {}", TypeGeneratorTest.prettyPrint(resultMap));

        final ObjectMapper mapper = new ObjectMapper();
        final RecursiveClass data = mapper.convertValue(
                resultMap.get("testObj"), RecursiveClass.class);
        assertThat(data, equalTo(new RecursiveClass(2)));
        
        Map<String, Object> expectedQueryData = mapper
                .convertValue(new RecursiveClass(2), Map.class);
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo(expectedQueryData));
    }
    
    @Test
    public void testRawList() {
        //TODO test getType(List<Integer>)
    }
    @Test
    public void testRawArray() {
        //TODO test getType(Integer[])
    }
    
    @Test
    public void testClassWithList() {
        testClassWithListOrArray("testClassWithList", new ClassWithLists(), ClassWithLists.class);
    }
    @Ignore("Need to fix this with arrays as lists")
    @Test
    public void testClassWithArray() {
        testClassWithListOrArray("testClassWithArray", new ClassWithRawArrays(), ClassWithRawArrays.class);
    }
    public void testClassWithListOrArray(String debug, Object testObject, Class<?> clazzUnderTest) {
        logger.debug("{}", debug);
        Object testType = testContext.getOutputType(clazzUnderTest);
        Assert.assertThat(testType, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) testType)
                        .name("testObj")
                        .staticValue(testObject)
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    strings"
        + "    ints"
        + "    objects {"
        + "      objName"
        + "      objIndex"
        + "      simple"
        + "      littleBBoolean"
        + "    }"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("{} results {}", debug, TypeGeneratorTest.prettyPrint(resultMap));
        
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> expectedQueryData = mapper
                .convertValue(testObject, Map.class);
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo(expectedQueryData));
    }
    
    @Test
    public void testListOfList() {
        logger.debug("testListOfList");
        Object listType = testContext.getOutputType(ClassWithListOfList.class);
        Assert.assertThat(listType, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) listType)
                        .name("testObj")
                        .staticValue(new ClassWithListOfList())
                        .build())
                .build();
        GraphQLSchema listTestSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    listOfListOfInts"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(listTestSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testCanonicalListOfList resultMap {}",
                TypeGeneratorTest.prettyPrint(resultMap));
        Object testObj = resultMap.get("testObj");
        Assert.assertThat(testObj, instanceOf(Map.class));
        Object listObj = ((Map<String, Object>) testObj).get("listOfListOfInts");
        Assert.assertThat(listObj, instanceOf(List.class));
    }
    
    
    @Ignore
    @Test
    public void testMaps() {
        logger.debug("testMaps");
        //Assert.fail();
    }
}
