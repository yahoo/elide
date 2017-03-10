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
public class FieldInputArgumentGenericsTest {
    private static Logger logger = LoggerFactory.getLogger(
            FieldInputArgumentGenericsTest.class);
    
    ITypeGenerator testContext = DefaultBuildContext.reflectionContext;

    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    @Test
    public void testArgumentGenerics() {
        logger.debug("testArgumentGenerics");
        Object testType = testContext.getOutputType(ArgumentsWithGenerics.class);
        Assert.assertThat(testType, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) testType)
                        .name("testObj")
                        .staticValue(new ArgumentsWithGenerics())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    sum (ints: [1,2,3])"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema)
                //.execute(TypeGeneratorTest.querySchema);
                .execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        logger.debug("testArgumentGenerics resultMap is {}", TypeGeneratorTest.prettyPrint(resultMap));
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo((Map<String, Object>) new HashMap<String, Object>() {{
                    put("sum", 6);
                }}));
    }
}
