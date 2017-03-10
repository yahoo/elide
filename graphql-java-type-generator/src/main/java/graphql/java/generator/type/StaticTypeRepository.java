package graphql.java.generator.type;

import graphql.introspection.Introspection.TypeKind;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The master place to store prebuilt, reusable GraphQL types.
 * @author dwinsor
 *
 */
public class StaticTypeRepository implements TypeRepository {
    private static Map<String, GraphQLOutputType> generatedOutputTypes =
            new ConcurrentHashMap<String, GraphQLOutputType>();
    private static Map<String, GraphQLInputType> generatedInputTypes =
            new ConcurrentHashMap<String, GraphQLInputType>();
    
    @Override
    public GraphQLType registerType(String typeName, GraphQLType graphQlType, TypeKind typeKind) {
        switch (typeKind) {
        case OBJECT:
        case INTERFACE:
            return registerType(typeName, (GraphQLOutputType) graphQlType);
        case INPUT_OBJECT:
            return registerType(typeName, (GraphQLInputType) graphQlType);
        default:
            return null;
        }
    }

    public GraphQLOutputType registerType(String typeName,
            GraphQLOutputType graphQlOutputType) {
        return (GraphQLOutputType) syncRegisterType(
                typeName, graphQlOutputType, generatedOutputTypes);
    }

    public GraphQLInputType registerType(String typeName,
            GraphQLInputType graphQlInputType) {
        return (GraphQLInputType) syncRegisterType(
                typeName, graphQlInputType, generatedInputTypes);
    }
    
    private <T extends GraphQLType> T syncRegisterType(String typeName,
            T graphQlType, Map<String, T> map) {
        synchronized (map) {
            if (!map.containsKey(typeName)) {
                map.put(typeName, graphQlType);
                return graphQlType;
            }
        }
        return map.get(typeName);

    }

    public Map<String, GraphQLOutputType> getGeneratedOutputTypes() {
        return generatedOutputTypes;
    }
    public Map<String, GraphQLInputType> getGeneratedInputTypes() {
        return generatedInputTypes;
    }
    
    /**
     * Resets the internal data of the TypeRepository to empty
     */
    public void clear() {
        //anyone working on the old types doesn't want to have
        //their generated*Types .clear()ed out from under them. 
        generatedOutputTypes =
                new ConcurrentHashMap<String, GraphQLOutputType>();
        generatedInputTypes =
                new ConcurrentHashMap<String, GraphQLInputType>();
    }

    @Override
    public GraphQLType getGeneratedType(String typeName, TypeKind typeKind) {
        switch (typeKind) {
        case OBJECT:
        case INTERFACE:
            return generatedOutputTypes.get(typeName);
        case INPUT_OBJECT:
            return generatedInputTypes.get(typeName);
        default:
            return null;
        }
    }
}
