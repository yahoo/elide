package graphql.java.generator.field;

import graphql.java.generator.type.ITypeGenerator;
import graphql.java.generator.type.TypeGeneratorTest;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.java.generator.*;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "serial"})
public class FieldInputArgumentTest {
    private static Logger logger = LoggerFactory.getLogger(
            FieldInputArgumentTest.class);
    
    ITypeGenerator testContext = DefaultBuildContext.reflectionContext;

    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    public void testMultipleMethodsDifferingArguments() {
        //TODO test get(), get(int), get(int, int) etc.
    }
    
    @Test
    public void testArgument() {
        logger.debug("testArgument");
        Object testType = testContext.getOutputType(ClassWithMethodsWithArguments.class);
        Assert.assertThat(testType, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) testType)
                        .name("testObj")
                        .staticValue(new ClassWithMethodsWithArguments())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    number (number: 100)"
        + "    number2Args (number: 20, ignored: false)"
        + "    stringConcat (one: \"abc\", two: \"def\")"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testArgument resultMap is {}", TypeGeneratorTest.prettyPrint(resultMap));
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo((Map<String, Object>) new HashMap<String, Object>() {{
                    put("number", 100);
                    put("number2Args", 20);
                    put("stringConcat", "abcdef");
                }}));
    }
}
