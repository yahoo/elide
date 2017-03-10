package graphql.java.generator.field.strategies;

import graphql.java.generator.strategies.ChainableStrategy;

public class FieldName_LowerCase
        extends ChainableStrategy<FieldNameStrategy>
        implements FieldNameStrategy {

    public FieldName_LowerCase(FieldNameStrategy nextStrategy) {
        setNextStrategy(nextStrategy);
    }
    
    @Override
    public String getFieldName(Object object) {
        if (getNextStrategy() == null) return null;
        String name = getNextStrategy().getFieldName(object);
        if (name == null) return null;
        return name.toLowerCase();
    }
}
