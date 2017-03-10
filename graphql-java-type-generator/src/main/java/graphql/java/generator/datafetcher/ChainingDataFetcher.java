package graphql.java.generator.datafetcher;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public abstract class ChainingDataFetcher<DF extends DataFetcher>
        implements ChainableDataFetcher<DF> {
    
    private DF nextFetcher;

    public ChainingDataFetcher(final DF nextFetcher) {
        this.setNextFetcher(nextFetcher);
    }
    
    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (getNextFetcher() == null) return null;
        return getNextFetcher().get(environment);
    }

    @Override
    public DF getNextFetcher() {
        return nextFetcher;
    }

    @Override
    public void setNextFetcher(DF nextFetcher) {
        this.nextFetcher = nextFetcher;
    }
}
