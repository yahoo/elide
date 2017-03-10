package graphql.java.generator.field.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.field.strategies.FieldTypeStrategy;
import graphql.introspection.Introspection.TypeKind;
import graphql.schema.GraphQLType;

public class FieldType_Reflection
        extends UnsharableBuildContextStorer
        implements FieldTypeStrategy {
    @Override
    public GraphQLType getTypeOfField(
            Object object, TypeKind typeKind) {
        if (object instanceof Field) {
            Field field = (Field) object;
            Class<?> fieldClazz = field.getType();
            Type genericType = field.getGenericType();
            return getTypeOfFieldFromSignature(
                    fieldClazz, genericType, field.toGenericString(), typeKind);
        }

        if (object instanceof Method) {
            Method method = (Method) object;
            Class<?> returnTypeClazz = method.getReturnType();
            Type genericType = method.getGenericReturnType();
            return getTypeOfFieldFromSignature(
                    returnTypeClazz, genericType, method.toGenericString(), typeKind);
        }

        return null;
    }
    
    protected GraphQLType getTypeOfFieldFromSignature(
            Class<?> typeClazz, Type genericType,
            String name, TypeKind typeKind) {

        ParameterizedType pType = null;
        //attempt GraphQLList from types
        if (genericType instanceof ParameterizedType) {
            pType = (ParameterizedType) genericType;
        }

        return getContext().getParameterizedType(typeClazz, pType, typeKind);
    }
}
