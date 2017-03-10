package graphql.java.generator.type.reflect;

import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.DefaultTypes;
import graphql.java.generator.Scalars;
import graphql.java.generator.type.strategies.DefaultTypeStrategy;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;

public class DefaultType_ReflectionScalarsLookup implements DefaultTypeStrategy {

    @Override
    public GraphQLType getDefaultType(Object object, TypeKind typeKind) {
        GraphQLScalarType scalar = getDefaultScalarType(object);
        if (scalar != null) return scalar;
        if (TypeKind.OBJECT.equals(typeKind)) {
            return getDefaultOutputType(object);
        }
        return null;
    }

    protected GraphQLOutputType getDefaultOutputType(Object object) {
        Class<?> clazz = ReflectionUtils.extractClassFromSupportedObject(object);
        if (clazz == null) return null;
        return DefaultTypes.getDefaultObjectType(clazz);
    }
    
    protected GraphQLScalarType getDefaultScalarType(Object object) {
        Class<?> clazz = ReflectionUtils.extractClassFromSupportedObject(object);
        if (clazz == null) return null;
        return Scalars.getScalarType(clazz);
    }
}
