package graphql.java.generator.argument.strategies;

import graphql.java.generator.argument.ArgContainer;
import graphql.java.generator.strategies.Strategy;

public interface ArgumentNameStrategy extends Strategy {

    String getArgumentName(ArgContainer container);
    
}
