package graphql.java.generator.argument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graphql.java.generator.BuildContext;
import graphql.java.generator.BuildContextAware;
import graphql.java.generator.argument.strategies.ArgumentStrategies;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputType;

public class ArgumentsGenerator implements BuildContextAware, IArgumentsGenerator {
    
    private final ArgumentStrategies strategies;

    public ArgumentsGenerator(ArgumentStrategies strategies) {
        this.strategies = strategies;
    }

    @Override
    public List<GraphQLArgument> getArguments(Object object) {
        List<GraphQLArgument> arguments = new ArrayList<GraphQLArgument>();
        List<ArgContainer> argObjects = getArgRepresentativeObjects(object);
        if (argObjects == null) {
            return arguments;
        }
        
        Set<String> argNames = new HashSet<String>();
        for (ArgContainer argObject : argObjects) {
            GraphQLArgument.Builder argBuilder = getArgument(argObject);
            if (argBuilder == null) {
                continue;
            }
            
            GraphQLArgument arg = argBuilder.build();
            if (argNames.contains(arg.getName())) {
                continue;
            }
            argNames.add(arg.getName());
            arguments.add(arg);
        }
        return arguments;
    }
    
    protected GraphQLArgument.Builder getArgument(ArgContainer argObject) {
        String name = getStrategies().getArgumentNameStrategy().getArgumentName(argObject);
        GraphQLInputType type = getStrategies().getArgumentTypeStrategy().getArgumentType(argObject);
        if (name == null || type == null) {
            return null;
        }
        
        String description = getStrategies().getArgumentDescriptionStrategy().getArgumentDescription(argObject);
        Object defaultValue = getStrategies().getArgumentDefaultValueStrategy().getArgumentDefaultValue(argObject);
        GraphQLArgument.Builder builder = GraphQLArgument.newArgument()
                .name(name)
                .type(type)
                .defaultValue(defaultValue)
                .description(description);
        return builder;
    }

    protected List<ArgContainer> getArgRepresentativeObjects(Object object) {
        return getStrategies().getArgumentObjectsStrategy()
                .getArgumentRepresentativeObjects(object);
    }

    @Override
    public BuildContext getContext() {
        return null;
    }

    @Override
    public void setContext(BuildContext context) {
        getStrategies().setContext(context);
    }

    @Override
    public ArgumentStrategies getStrategies() {
        return strategies;
    }
}
