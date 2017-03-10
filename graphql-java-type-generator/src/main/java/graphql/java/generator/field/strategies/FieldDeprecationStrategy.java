package graphql.java.generator.field.strategies;

import graphql.java.generator.strategies.Strategy;

public interface FieldDeprecationStrategy extends Strategy {

    String getFieldDeprecation(Object object);
    
}
