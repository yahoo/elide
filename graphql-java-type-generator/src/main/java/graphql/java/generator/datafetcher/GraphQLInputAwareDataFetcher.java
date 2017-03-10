package graphql.java.generator.datafetcher;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;

public class GraphQLInputAwareDataFetcher
        extends ChainingDataFetcher<ArgAwareDataFetcher> {
    
    private List<GraphQLArgument> arguments;

    public GraphQLInputAwareDataFetcher(final ArgAwareDataFetcher nextFetcher) {
        super(nextFetcher);
    }
    
    public GraphQLInputAwareDataFetcher(final ArgAwareDataFetcher nextFetcher,
            final List<GraphQLArgument> arguments) {
        super(nextFetcher);
        this.setArguments(arguments);
    }
    
    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (getArguments() != null) {
            String[] originalArgNames = new String[getArguments().size()];
            for (int index = 0; index < originalArgNames.length; ++index) {
                originalArgNames[index] = getArguments().get(index).getName();
            }
            getNextFetcher().setArgNames(originalArgNames);
        }
        return getNextFetcher().get(environment);
    }

    public List<GraphQLArgument> getArguments() {
        return arguments;
    }

    public void setArguments(List<GraphQLArgument> arguments) {
        this.arguments = arguments;
    }
}
