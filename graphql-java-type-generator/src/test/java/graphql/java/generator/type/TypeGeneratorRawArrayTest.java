package graphql.java.generator.type;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.java.generator.ClassWithArray;
import graphql.java.generator.ClassWithArrayList;
import graphql.java.generator.DefaultBuildContext;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.Map;

public class TypeGeneratorRawArrayTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorRawArrayTest.class);

    ITypeGenerator generator = DefaultBuildContext.reflectionContext;

    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }

    @Test
    public void testRawArray() {
        logger.debug("testRawArray");
        Object objectType = generator.getOutputType(ClassWithArray.class);
        assertListOfInt(objectType);
        assertQueryOnListOfInt((GraphQLOutputType) objectType, new ClassWithArray());
    }

    @Test
    public void testArrayList() {
        logger.debug("testArrayList");
        Object objectType = generator.getOutputType(ClassWithArrayList.class);
        assertListOfInt(objectType);
        assertQueryOnListOfInt((GraphQLOutputType) objectType, new ClassWithArrayList());
    }

    public void assertListOfInt(Object objectType) {
        Assert.assertThat(objectType, instanceOf(GraphQLObjectType.class));
        Assert.assertThat(objectType, not(instanceOf(GraphQLList.class)));
        GraphQLFieldDefinition fieldDefinition = ((GraphQLObjectType) objectType).getFieldDefinition("integers");
        Assert.assertThat(fieldDefinition, notNullValue());
        GraphQLOutputType outputType = fieldDefinition.getType();

        Assert.assertThat(outputType, CoreMatchers.instanceOf(GraphQLList.class));
        GraphQLType wrappedType = ((GraphQLList) outputType).getWrappedType();
        Assert.assertThat(wrappedType, instanceOf(GraphQLScalarType.class));
        Assert.assertThat((GraphQLScalarType)wrappedType, is(Scalars.GraphQLInt));
    }
    
    @SuppressWarnings("unchecked")
    public void assertQueryOnListOfInt(GraphQLOutputType outputType, Object staticValue) {
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type(outputType)
                        .name("testObj")
                        .staticValue(staticValue)
                        .build())
                .build();
        GraphQLSchema listTestSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{" +
        "  testObj {" +
        "    integers" +
        "  }" +
        "}";
        ExecutionResult queryResult = new GraphQL(listTestSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("assertQueryOnListOfInt resultMap {}",
                TypeGeneratorTest.prettyPrint(resultMap));
        
        Object testObj = resultMap.get("testObj");
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> expectedQueryData = mapper
                .convertValue(staticValue, Map.class);
        Assert.assertThat((Map<String, Object>)testObj,
                equalTo(expectedQueryData));
    }
}