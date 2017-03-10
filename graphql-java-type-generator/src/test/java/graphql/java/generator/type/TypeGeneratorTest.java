package graphql.java.generator.type;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.java.generator.DefaultBuildContext;
import graphql.java.generator.InterfaceChild;
import graphql.java.generator.InterfaceImpl;
import graphql.java.generator.InterfaceImplSecondary;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "serial"})
public class TypeGeneratorTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorTest.class);
    
    ITypeGenerator generator = DefaultBuildContext.reflectionContext;
    
    public static String querySchema = ""
        + "query IntrospectionQuery {"
        + "  __schema {"
        + "    queryType {"
        + "      name"
        + "    }"
        + "    mutationType {"
        + "      name"
        + "    }"
        + "    types {"
        + "      ...FullType"
        + "    }"
        + "    directives {"
        + "      name"
        + "      description"
        + "      args {"
        + "        ...InputValue"
        + "      }"
        + "      onOperation"
        + "      onFragment"
        + "      onField"
        + "    }"
        + "  }"
        + "}"
        + ""
        + "fragment FullType on __Type {"
        + "  kind"
        + "  name"
        + "  description"
        + "  fields(includeDeprecated: true) {"
        + "    name"
        + "    description"
        + "    args {"
        + "      ...InputValue"
        + "    }"
        + "    type {"
        + "      ...TypeRef"
        + "    }"
        + "    isDeprecated"
        + "    deprecationReason"
        + "  }"
        + "  inputFields {"
        + "    ...InputValue"
        + "  }"
        + "  interfaces {"
        + "    ...TypeRef"
        + "  }"
        + "  enumValues(includeDeprecated: true) {"
        + "    name"
        + "    description"
        + "    isDeprecated"
        + "    deprecationReason"
        + "  }"
        + "  possibleTypes {"
        + "    ...TypeRef"
        + "  }"
        + "}"
        + ""
        + "fragment InputValue on __InputValue {"
        + "  name"
        + "  description"
        + "  type {"
        + "    ...TypeRef"
        + "  }"
        + "  defaultValue"
        + "}"
        + ""
        + "fragment TypeRef on __Type {"
        + "  kind"
        + "  name"
        + "  ofType {"
        + "    kind"
        + "    name"
        + "    ofType {"
        + "      kind"
        + "      name"
        + "      ofType {"
        + "        kind"
        + "        name"
        + "      }"
        + "    }"
        + "  }"
        + "}";
    
    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    @Test
    public void testClassesWithInterfaces() {
        logger.debug("testClassesWithInterfaces");
        Object interfaceType = generator.getOutputType(InterfaceChild.class);
        assertThat(interfaceType, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) interfaceType)
                        .name("testObj")
                        .staticValue(new InterfaceImpl())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    parent"
        + "    child"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        
        final ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> expectedQueryData = mapper
                .convertValue(new InterfaceImpl(), Map.class);
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo(expectedQueryData));
        assertThat(((Map<String, Object>)resultMap.get("testObj")).size(),
                is(2));
    }
    
    @Test
    public void testClassesWithInterfacesSecondary() throws JsonProcessingException {
        logger.debug("testClassesWithInterfacesSecondary");
        Object impl = generator.getOutputType(InterfaceChild.class);
        assertThat(impl, instanceOf(GraphQLOutputType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) impl)
                        .name("testObj")
                        .staticValue(new InterfaceImplSecondary())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    parent"
        + "    child"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        if (logger.isDebugEnabled()) {
            logger.debug("testClassesWithInterfacesSecondary resultMap {}", prettyPrint(resultMap));
        }
        
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo((Map<String, Object>) new HashMap<String, Object>() {{
                    put("parent", "parent2");
                    put("child", "child2");
                }}));
        assertThat(((Map<String, Object>)resultMap.get("testObj")).size(),
                is(2));
    }
    
    @Test
    public void testGraphQLInterfaces() throws JsonProcessingException {
        logger.debug("testGraphQLInterfaces");
        Object interfaceType = generator.getInterfaceType(InterfaceChild.class);
        assertThat(interfaceType, instanceOf(GraphQLInterfaceType.class));
        
        GraphQLObjectType queryType = newObject()
                .name("testQuery")
                .field(newFieldDefinition()
                        .type((GraphQLOutputType) interfaceType)
                        .name("testObj")
                        .staticValue(new InterfaceImpl())
                        .build())
                .build();
        GraphQLSchema testSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
        
        String queryString = 
        "{"
        + "  testObj {"
        + "    parent"
        + "    child"
        + "  }"
        + "}";
        ExecutionResult queryResult = new GraphQL(testSchema).execute(queryString);
        assertThat(queryResult.getErrors(), is(empty()));
        Map<String, Object> resultMap = (Map<String, Object>) queryResult.getData();
        if (logger.isDebugEnabled()) {
            logger.debug("testGraphQLInterfaces resultMap {}", prettyPrint(resultMap));
        }
        
        assertThat(((Map<String, Object>)resultMap.get("testObj")),
                equalTo((Map<String, Object>) new HashMap<String, Object>() {{
                    put("parent", "parent");
                    put("child", "child");
                }}));
        assertThat(((Map<String, Object>)resultMap.get("testObj")).size(),
                is(2));
    }
    
    public static ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    public static String prettyPrint(Map<String, Object> resultMap) {
        try {
            return mapper.writeValueAsString(resultMap);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return resultMap.toString();
        }
    }
}
