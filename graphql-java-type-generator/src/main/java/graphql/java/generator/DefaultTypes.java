package graphql.java.generator;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

public class DefaultTypes {
    
    private static final GraphQLObjectType emptyJavaObject = GraphQLObjectType.newObject()
            .name("java_lang_Object")
            .description("Default Type for java.lang.Object")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("toString")
                    .type(graphql.Scalars.GraphQLString)
                    .dataFetcher(new DataFetcher() {
                        @Override
                        public Object get(DataFetchingEnvironment environment) {
                            if (environment.getSource() == null) return null;
                            return environment.getSource().toString();
                        }
                    })
                    .build())
            .build();

    public static GraphQLObjectType getDefaultObjectType(Class<?> clazz) {
        if (Object.class.equals(clazz)) {
            return emptyJavaObject;
        }
        return null;
    }
    
}
