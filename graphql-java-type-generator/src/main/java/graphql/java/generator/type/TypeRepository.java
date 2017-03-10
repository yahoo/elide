package graphql.java.generator.type;

import graphql.introspection.Introspection.TypeKind;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.Map;

/**
 * The master place to store prebuilt, reusable GraphQL types.
 * Must support concurrency.
 * @author dwinsor
 *
 */
public interface TypeRepository {
    public GraphQLType registerType(String typeName,
            GraphQLType graphQlType, TypeKind typeKind);
    
    public GraphQLOutputType registerType(String typeName,
            GraphQLOutputType graphQlOutputType);

    public GraphQLInputType registerType(String typeName,
            GraphQLInputType graphQlInputType);

    public Map<String, GraphQLOutputType> getGeneratedOutputTypes();
    public Map<String, GraphQLInputType> getGeneratedInputTypes();
    public GraphQLType getGeneratedType(String typeName, TypeKind typeKind);
    
    /**
     * Resets the internal data of the TypeRepository to empty.
     * Must be done in a way that does not impact BuildContexts currently running.
     */
    public void clear();
}
