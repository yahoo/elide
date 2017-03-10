package graphql.java.generator.field.strategies;

import graphql.java.generator.strategies.Strategy;

public interface FieldDefaultValueStrategy extends Strategy {

    String getFieldDefaultValue(Object object);
    
}
