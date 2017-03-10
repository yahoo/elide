package graphql.java.generator.field;

import graphql.java.generator.BuildContext;
import graphql.java.generator.BuildContextAware;
import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.datafetcher.ArgAwareDataFetcher;
import graphql.java.generator.datafetcher.ArgumentExtractingDataFetcher;
import graphql.java.generator.datafetcher.GraphQLInputAwareDataFetcher;
import graphql.java.generator.field.strategies.FieldDataFetcherStrategy;
import graphql.schema.GraphQLArgument;

import java.util.List;

/**
 * If a DataFetcher is {@link ArgAwareDataFetcher}, and if {@link GraphQLArgument}s
 * can be obtained, then chain these DataFetchers with Argument info.
 * @author dwinsor
 *
 */
public class FieldDataFetcher_InputAndArgAware
        extends UnsharableBuildContextStorer
        implements FieldDataFetcherStrategy {
    
    private FieldDataFetcherStrategy nextStrategy;
    
    public FieldDataFetcher_InputAndArgAware(FieldDataFetcherStrategy nextStrategy) {
        this.setNextStrategy(nextStrategy);
    }
    
    @Override
    public Object getFieldFetcher(Object object) {
        Object fetcher = getNextStrategy().getFieldFetcher(object);
        if (!(fetcher instanceof ArgAwareDataFetcher)) {
            return fetcher;
        }
        
        List<GraphQLArgument> arguments = getContext()
                .getArgumentsGeneratorStrategy().getArguments(object);
        if (arguments == null || arguments.isEmpty()) {
            return fetcher;
        }
        return new GraphQLInputAwareDataFetcher(
                new ArgumentExtractingDataFetcher((ArgAwareDataFetcher) fetcher), arguments);
    }

    public FieldDataFetcherStrategy getNextStrategy() {
        return nextStrategy;
    }

    public void setNextStrategy(FieldDataFetcherStrategy nextStrategy) {
        this.nextStrategy = nextStrategy;
    }
    
    @Override
    public void setContext(BuildContext context) {
        super.setContext(context);
        if (getNextStrategy() instanceof BuildContextAware) {
            ((BuildContextAware) getNextStrategy()).setContext(context);
        }
    }
}
