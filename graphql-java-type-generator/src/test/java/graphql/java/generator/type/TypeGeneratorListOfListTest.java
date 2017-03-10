package graphql.java.generator.type;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.java.generator.ClassWithListOfList;
import graphql.java.generator.ClassWithListOfListOfList;
import graphql.java.generator.DefaultBuildContext;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import static org.junit.Assert.assertThat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "serial"})
public class TypeGeneratorListOfListTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorListOfListTest.class);
    
    ITypeGenerator generator = DefaultBuildContext.reflectionContext;
    
    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    @Test
    public void testGeneratedListOfList() {
        logger.debug("testGeneratedListOfList");
        Object objectType = generator.getOutputType(ClassWithListOfList.class);
        assertClassWithListOfList(objectType);
    }
    
    public static void assertClassWithListOfList(Object objectType) {
        Assert.assertThat(objectType, instanceOf(GraphQLObjectType.class));
        Assert.assertThat(objectType, not(instanceOf(GraphQLList.class)));
        GraphQLFieldDefinition field = ((GraphQLObjectType) objectType)
                .getFieldDefinition("listOfListOfInts");
        
        Assert.assertThat(field, notNullValue());
        GraphQLOutputType outputType = field.getType();
        assertListOfListOfInt(outputType);
    }
    
    @Test
    public void testGeneratedListOfListOfList() {
        logger.debug("testGeneratedListOfListOfList");
        Object objectType = generator.getOutputType(ClassWithListOfListOfList.class);
        Assert.assertThat(objectType, instanceOf(GraphQLObjectType.class));
        Assert.assertThat(objectType, not(instanceOf(GraphQLList.class)));
        GraphQLFieldDefinition field = ((GraphQLObjectType) objectType)
                .getFieldDefinition("listOfListOfListOfInts");
        
        Assert.assertThat(field, notNullValue());
        GraphQLOutputType listType = field.getType();
        Assert.assertThat(listType, instanceOf(GraphQLList.class));
        GraphQLType wrappedType = ((GraphQLList) listType).getWrappedType();
        assertListOfListOfInt(wrappedType);
    }
    
    public static void assertListOfListOfInt(GraphQLType type) {
        Assert.assertThat(type, instanceOf(GraphQLList.class));
        GraphQLType wrappedType = ((GraphQLList) type).getWrappedType();
        Assert.assertThat(wrappedType, instanceOf(GraphQLList.class));
        GraphQLType integerType = ((GraphQLList) wrappedType).getWrappedType();
        Assert.assertThat(integerType, instanceOf(GraphQLScalarType.class));
    }
    
    @Test
    public void testCanonicalListOfList() {
        logger.debug("testCanonicalListOfList");
        List<List<Integer>> listOfListOfInts = new ArrayList<List<Integer>>() {{
            add(new ArrayList<Integer>() {{
                add(0);
            }});
        }};
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type(new GraphQLList(new GraphQLList(Scalars.GraphQLInt)))
                        .name("testObj")
                        .staticValue(listOfListOfInts)
                        .build())
                .build();
        GraphQLSchema listTestSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{" +
        "  testObj" +
        "}";
        ExecutionResult queryResult = new GraphQL(listTestSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testCanonicalListOfList resultMap {}",
                TypeGeneratorTest.prettyPrint(resultMap));
        Object testObj = resultMap.get("testObj");
        Assert.assertThat(testObj, instanceOf(List.class));
        assertThat(((List<List<Integer>>) testObj),
                equalTo(listOfListOfInts));
        
    }
}
