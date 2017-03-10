package graphql.java.generator;

import graphql.java.generator.argument.ArgumentsGenerator;
import graphql.java.generator.argument.strategies.ArgumentStrategies;
import graphql.java.generator.field.FieldsGenerator;
import graphql.java.generator.field.strategies.FieldStrategies;
import graphql.java.generator.type.FullTypeGenerator;
import graphql.java.generator.type.StaticTypeRepository;
import graphql.java.generator.type.TypeRepository;
import graphql.java.generator.type.WrappingTypeGenerator;
import graphql.java.generator.type.strategies.TypeStrategies;

public class DefaultBuildContext {
    public static final TypeRepository defaultTypeRepository =
            new StaticTypeRepository();
    public static final BuildContext reflectionContext = 
            newReflectionContext();
    
    public static BuildContext newReflectionContext() {
        return new BuildContext.Builder()
                .setTypeGeneratorStrategy(
                        new WrappingTypeGenerator(new FullTypeGenerator(new TypeStrategies.Builder()
                        .usingTypeRepository(defaultTypeRepository)
                        .build())))
                .setFieldsGeneratorStrategy(
                        new FieldsGenerator(new FieldStrategies.Builder()
                        .build()))
                .setArgumentsGeneratorStrategy(
                        new ArgumentsGenerator(new ArgumentStrategies.Builder()
                        .build()))
                .build();
    }
}
