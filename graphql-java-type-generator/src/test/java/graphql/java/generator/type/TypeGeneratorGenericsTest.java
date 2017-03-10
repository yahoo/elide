package graphql.java.generator.type;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.java.generator.ClassWithListOfGenerics;
import graphql.java.generator.ClassWithListOfGenericsWithBounds;
import graphql.java.generator.DefaultBuildContext;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import static org.junit.Assert.assertThat;

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

import java.util.Map;

@SuppressWarnings({"unchecked"})
public class TypeGeneratorGenericsTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorGenericsTest.class);
    
    ITypeGenerator generator = DefaultBuildContext.reflectionContext;
    
    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    @Test
    public void testGenericsBoundsQuery() {
        logger.debug("testGenericsBoundsQuery");
        Object objectType = generator.getOutputType(ClassWithListOfGenericsWithBounds.class);
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) objectType)
                        .name("testObj")
                        .staticValue(new ClassWithListOfGenericsWithBounds())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    listOfParamOfII {"
        + "      interfaceImpl {"
        + "        parent"
        + "        child"
        + "      }"
        + "    }"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema)
                //.execute(TypeGeneratorTest.querySchema);
                .execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testGenericsBoundsQuery results {}",
                TypeGeneratorTest.prettyPrint(resultMap));
        
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> expectedQueryData = mapper
                .convertValue(new ClassWithListOfGenericsWithBounds(), Map.class);
        logger.debug("testGenericsBoundsQuery expectedQueryData {}",
                TypeGeneratorTest.prettyPrint(expectedQueryData));
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo(expectedQueryData));
    }
    
    @Test
    public void testGeneratedListOfParam() {
        logger.debug("testGeneratedListOfParam");
        Object objectType = generator.getOutputType(ClassWithListOfGenerics.class);
        Assert.assertThat(objectType, instanceOf(GraphQLObjectType.class));
        Assert.assertThat(objectType, not(instanceOf(GraphQLList.class)));
        GraphQLFieldDefinition field = ((GraphQLObjectType) objectType)
                .getFieldDefinition("listOfParamOfInts");
        
        Assert.assertThat(field, notNullValue());
        GraphQLOutputType listType = field.getType();
        Assert.assertThat(listType, instanceOf(GraphQLList.class));
        GraphQLType wrappedType = ((GraphQLList) listType).getWrappedType();
        Assert.assertThat(wrappedType, instanceOf(GraphQLObjectType.class));
    }
}
