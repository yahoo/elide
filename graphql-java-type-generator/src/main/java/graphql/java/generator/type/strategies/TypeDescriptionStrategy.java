package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;

/**
 * Given any type-representative object,
 * decide how you wish the GraphQL description to be.
 * @author dwinsor
 *
 */
public interface TypeDescriptionStrategy extends Strategy {
    /**
     * 
     * @param object A representative "type" object, the exact type of which is contextual
     * @return May return null in which case the description will not be set.
     */
    public String getTypeDescription(Object object);
}
