package graphql.java.generator.type;

import java.lang.reflect.ParameterizedType;

import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;


public interface ITypeGenerator {
    
    TypeStrategies getStrategies();
    
    /**
     * @param object A representative "object" from which to construct
     * a {@link GraphQLOutputType}, the exact type of which is contextual
     * @return
     */
    GraphQLOutputType getOutputType(Object object);
    
    /**
     * @param object A representative "object" from which to construct
     * a {@link GraphQLInputType}, the exact type of which is contextual
     * @return
     */
    GraphQLInputType getInputType(Object object);
    
    /**
     * @param object A representative "object" from which to construct
     * a {@link GraphQLInterfaceType}, the exact type of which is contextual
     * @return
     */
    GraphQLInterfaceType getInterfaceType(Object object);

    GraphQLType getParameterizedType(Object object, ParameterizedType type, TypeKind typeKind);
    
}