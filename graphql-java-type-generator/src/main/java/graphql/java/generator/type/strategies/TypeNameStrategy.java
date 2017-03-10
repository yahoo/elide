package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;
import graphql.java.generator.type.TypeRepository;

/**
 * Given any object, decide how you wish the GraphQL type to be named
 * @author dwinsor
 *
 */
public interface TypeNameStrategy extends Strategy {
    /**
     * 
     * @param object A representative "type" object, the exact type of which is contextual
     * @return May return null if no suitable type name is possible, which
     * indicates this type will not be placed in the {@link TypeRepository}.
     */
    public String getTypeName(Object object);
}
