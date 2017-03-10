package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;
import graphql.schema.GraphQLEnumValueDefinition;

import java.util.List;

public interface EnumValuesStrategy extends Strategy {
    /**
     * 
     * @param object A representative "type" object, the exact type of which is contextual
     * @return Must return null if not an enum.
     */
    List<GraphQLEnumValueDefinition> getEnumValueDefinitions(Object object);
}
