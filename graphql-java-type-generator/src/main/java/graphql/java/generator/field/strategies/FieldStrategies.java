package graphql.java.generator.field.strategies;

import java.util.HashMap;

import graphql.java.generator.field.FieldDataFetcher_InputAndArgAware;
import graphql.java.generator.field.reflect.FieldDataFetcher_Reflection;
import graphql.java.generator.field.reflect.FieldDefaultValue_Reflection;
import graphql.java.generator.field.reflect.FieldDeprecation_Reflection;
import graphql.java.generator.field.reflect.FieldDescription_ReflectionAutogen;
import graphql.java.generator.field.reflect.FieldName_Reflection;
import graphql.java.generator.field.reflect.FieldObjects_Reflection;
import graphql.java.generator.field.reflect.FieldType_Reflection;
import graphql.java.generator.strategies.AbstractStrategiesContainer;
import graphql.java.generator.strategies.Strategy;

public class FieldStrategies extends AbstractStrategiesContainer {
    public FieldObjectsStrategy getFieldObjectsStrategy() {
        return (FieldObjectsStrategy) allStrategies.get(FieldObjectsStrategy.class);
    }

    public FieldDataFetcherStrategy getFieldDataFetcherStrategy() {
        return (FieldDataFetcherStrategy) allStrategies.get(FieldDataFetcherStrategy.class);
    }
    
    public FieldNameStrategy getFieldNameStrategy() {
        return (FieldNameStrategy) allStrategies.get(FieldNameStrategy.class);
    }
    
    public FieldTypeStrategy getFieldTypeStrategy() {
        return (FieldTypeStrategy) allStrategies.get(FieldTypeStrategy.class);
    }
    
    public FieldDescriptionStrategy getFieldDescriptionStrategy() {
        return (FieldDescriptionStrategy) allStrategies.get(FieldDescriptionStrategy.class);
    }
    
    public FieldDeprecationStrategy getFieldDeprecationStrategy() {
        return (FieldDeprecationStrategy) allStrategies.get(FieldDeprecationStrategy.class);
    }
    
    public FieldDefaultValueStrategy getFieldDefaultValueStrategy() {
        return (FieldDefaultValueStrategy) allStrategies.get(FieldDefaultValueStrategy.class);
    }
    
    
    public static class Builder {
        @SuppressWarnings("serial")
        private HashMap<Class<? extends Strategy>, Strategy> strategies =
                new HashMap<Class<? extends Strategy>, Strategy>() {{
                    put(FieldObjectsStrategy.class, new FieldObjects_Reflection());
                    put(FieldNameStrategy.class, new FieldName_Reflection());
                    put(FieldTypeStrategy.class, new FieldType_Reflection());
                    put(FieldDataFetcherStrategy.class, new FieldDataFetcher_InputAndArgAware(new FieldDataFetcher_Reflection()));
                    put(FieldDescriptionStrategy.class, new FieldDescription_ReflectionAutogen());
                    put(FieldDefaultValueStrategy.class, new FieldDefaultValue_Reflection());
                    put(FieldDeprecationStrategy.class, new FieldDeprecation_Reflection());
                }};

        public Builder fieldObjectsStrategy(FieldObjectsStrategy fieldObjectsStrategy) {
            strategies.put(FieldObjectsStrategy.class, fieldObjectsStrategy);
            return this;
        }
        
        public Builder fieldDataFetcherStrategy(FieldDataFetcherStrategy fieldDataFetcherStrategy) {
            strategies.put(FieldDataFetcherStrategy.class, fieldDataFetcherStrategy);
            return this;
        }
        
        public Builder fieldNameStrategy(FieldNameStrategy fieldNameStrategy) {
            strategies.put(FieldNameStrategy.class, fieldNameStrategy);
            return this;
        }
        
        public Builder fieldTypeStrategy(FieldTypeStrategy fieldTypeStrategy) {
            strategies.put(FieldTypeStrategy.class, fieldTypeStrategy);
            return this;
        }
        
        public Builder fieldDescriptionStrategy(FieldDescriptionStrategy fieldDescriptionStrategy) {
            strategies.put(FieldDescriptionStrategy.class, fieldDescriptionStrategy);
            return this;
        }
        
        public Builder fieldDeprecationStrategy(FieldDeprecationStrategy fieldDeprecationStrategy) {
            strategies.put(FieldDeprecationStrategy.class, fieldDeprecationStrategy);
            return this;
        }
        
        public Builder fieldDefaultValueStrategy(FieldDefaultValueStrategy fieldDefaultValueStrategy) {
            strategies.put(FieldDefaultValueStrategy.class, fieldDefaultValueStrategy);
            return this;
        }
        
        public Builder additionalStrategy(Strategy strategy) {
            strategies.put(strategy.getClass(), strategy);
            return this;
        }
        
        public FieldStrategies build() {
            return new FieldStrategies(this);
        }
    }
    
    private FieldStrategies(Builder builder) {
        allStrategies.putAll(builder.strategies);
    }
}
