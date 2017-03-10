package graphql.java.generator.field.strategies;

import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.strategies.Strategy;
import graphql.schema.GraphQLType;

public interface FieldTypeStrategy extends Strategy {
    /**
     * 
     * @param object A representative "field" object, the exact type of which is contextual
     * @param typeKind
     * @return
     */
    GraphQLType getTypeOfField(Object object, TypeKind typeKind);
}
