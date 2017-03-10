package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;
import graphql.schema.TypeResolver;

public interface TypeResolverStrategy extends Strategy {

    TypeResolver getTypeResolver(Object object);
    
}
