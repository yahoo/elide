package graphql.java.generator.datafetcher;

import graphql.schema.DataFetcher;

public interface ArgAwareDataFetcher extends DataFetcher {
    
    Object[] getArgValues();
    
    void setArgValues(Object[] values);
    
    String[] getArgNames();
    
    void setArgNames(String[] names);
    
}