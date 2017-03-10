package graphql.java.generator.datafetcher;

import graphql.schema.DataFetchingEnvironment;

public class ArgumentExtractingDataFetcher
        extends ArgumentAwareDataFetcher
        implements ChainableDataFetcher<ArgAwareDataFetcher> {
    
    private ArgAwareDataFetcher nextFetcher;

    public ArgumentExtractingDataFetcher(final ArgAwareDataFetcher nextFetcher) {
        this.setNextFetcher(nextFetcher);
    }
    
    public ArgumentExtractingDataFetcher(final ArgAwareDataFetcher nextFetcher, final String[] argNames) {
        this.setNextFetcher(nextFetcher);
        this.setArgNames(argNames);
    }
    
    @Override
    public Object get(DataFetchingEnvironment environment) {
        String[] originalArgNames = getArgNames();
        Object[] extractedArgs = new Object[originalArgNames.length];
        for (int index = 0; index < originalArgNames.length; ++index) {
            extractedArgs[index] = environment.getArgument(originalArgNames[index]);
            System.out.println("arg" + index + " " + extractedArgs[index].getClass());
        }
        getNextFetcher().setArgValues(extractedArgs);
        getNextFetcher().setArgNames(originalArgNames);
        return getNextFetcher().get(environment);
    }

    @Override
    public ArgAwareDataFetcher getNextFetcher() {
        return nextFetcher;
    }

    @Override
    public void setNextFetcher(ArgAwareDataFetcher nextFetcher) {
        this.nextFetcher = nextFetcher;
    }
}
