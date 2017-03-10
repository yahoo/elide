package graphql.java.generator.field.strategies;

import graphql.java.generator.strategies.Strategy;

/**
 * Given any field-representative object,
 * decide how you wish the GraphQL description to be.
 * @author dwinsor
 *
 */
public interface FieldDescriptionStrategy extends Strategy {
    /**
     * 
     * @param object A representative "field" object, the exact type of which is contextual
     * @return May return null in which case the description will not be set.
     */
    public String getFieldDescription(Object object);
}
