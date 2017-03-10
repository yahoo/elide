package graphql.java.generator.field.strategies;

import graphql.java.generator.strategies.Strategy;

public interface FieldDataFetcherStrategy extends Strategy {
    /**
     * Return the {@linkplain graphql.schema.DataFetcher DataFetcher} instance that will fetch the data,
     * OR return some other object instance to return a static value.
     * 
     * @param object A representative "field" object, the exact type of which is contextual
     * @return May return null to leave the {@linkplain graphql.schema.DataFetcher DataFetcher} blank
     * while creating the field definition.
     */
    Object getFieldFetcher(Object object);
}
