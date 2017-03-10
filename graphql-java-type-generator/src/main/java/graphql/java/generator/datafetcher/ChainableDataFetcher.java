package graphql.java.generator.datafetcher;

import graphql.schema.DataFetcher;

public interface ChainableDataFetcher<DF extends DataFetcher> extends DataFetcher {
    
    DF getNextFetcher();
    
    void setNextFetcher(DF nextFetcher);
    
}