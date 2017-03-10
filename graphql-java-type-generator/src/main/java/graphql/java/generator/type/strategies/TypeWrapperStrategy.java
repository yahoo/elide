package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;
import graphql.java.generator.type.TypeSpecContainer;
import graphql.schema.GraphQLType;

public interface TypeWrapperStrategy extends Strategy {

    /**
     * If the given {@code interiorType} is a representative "type"
     * object that should be wrapped (e.g. the {@code Integer} from {@code List<Integer>})
     * then return a GraphQL Wrapper around the generated GraphQLType
     * (e.g. GraphQLList(interiorType) ).
     * Return original object if no wrapper is applicable.
     * @param interiorType
     * @param originalObject
     * @return
     */
    GraphQLType wrapType(
            GraphQLType interiorType, TypeSpecContainer originalObject);
    
    /**
     * If the given {@code originalObject} is a representative "type"
     * object that should be wrapped (e.g. class of {@code List<Integer>})
     * then return the interior type spec to build (e.g. class of {@code Integer})
     * Return null if no wrapper is applicable.
     * @param originalObject
     * @return
     */
    TypeSpecContainer getInteriorObjectToGenerate(
            TypeSpecContainer originalObject);
    
}
