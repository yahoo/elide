package graphql.java.generator.type.strategies;

import java.util.HashMap;

import graphql.java.generator.strategies.AbstractStrategiesContainer;
import graphql.java.generator.strategies.Strategy;
import graphql.java.generator.type.TypeRepository;
import graphql.java.generator.type.reflect.DefaultType_ReflectionScalarsLookup;
import graphql.java.generator.type.reflect.EnumValues_Reflection;
import graphql.java.generator.type.reflect.Interfaces_Reflection;
import graphql.java.generator.type.reflect.TypeDescription_ReflectionAutogen;
import graphql.java.generator.type.reflect.TypeName_ReflectionFQNReplaceDotWithChar;
import graphql.java.generator.type.reflect.TypeWrapper_ReflectionList;
import graphql.java.generator.type.resolver.TypeResolverStrategy_Caching;

public class TypeStrategies extends AbstractStrategiesContainer {
    private final TypeRepository typeRepository;

    public TypeRepository getTypeRepository() {
        return typeRepository;
    }

    public TypeNameStrategy getTypeNameStrategy() {
        return (TypeNameStrategy) allStrategies.get(TypeNameStrategy.class);
    }
    public TypeDescriptionStrategy getTypeDescriptionStrategy() {
        return (TypeDescriptionStrategy) allStrategies.get(TypeDescriptionStrategy.class);
    }
    public DefaultTypeStrategy getDefaultTypeStrategy() {
        return (DefaultTypeStrategy) allStrategies.get(DefaultTypeStrategy.class);
    }
    public EnumValuesStrategy getEnumValuesStrategy() {
        return (EnumValuesStrategy) allStrategies.get(EnumValuesStrategy.class);
    }
    public InterfacesStrategy getInterfacesStrategy() {
        return (InterfacesStrategy) allStrategies.get(InterfacesStrategy.class);
    }
    public TypeResolverStrategy getTypeResolverStrategy() {
        return (TypeResolverStrategy) allStrategies.get(TypeResolverStrategy.class);
    }
    public TypeWrapperStrategy getTypeWrapperStrategy() {
        return (TypeWrapperStrategy) allStrategies.get(TypeWrapperStrategy.class);
    }
    
    public static class Builder {
        @SuppressWarnings("serial")
        private HashMap<Class<? extends Strategy>, Strategy> strategies =
                new HashMap<Class<? extends Strategy>, Strategy>() {{
                    put(DefaultTypeStrategy.class, new DefaultType_ReflectionScalarsLookup());
                    put(TypeNameStrategy.class, new TypeName_ReflectionFQNReplaceDotWithChar());
                    put(TypeDescriptionStrategy.class, new TypeDescription_ReflectionAutogen());
                    put(EnumValuesStrategy.class, new EnumValues_Reflection());
                    put(InterfacesStrategy.class, new Interfaces_Reflection());
                    put(TypeResolverStrategy.class, new TypeResolverStrategy_Caching());
                    put(TypeWrapperStrategy.class, new TypeWrapper_ReflectionList());
                }};
        protected TypeRepository typeRepository;
        
        public Builder usingTypeRepository(TypeRepository typeRepository) {
            this.typeRepository = typeRepository;
            return this;
        }

        public Builder typeNameStrategy(TypeNameStrategy typeNameStrategy) {
            strategies.put(TypeNameStrategy.class, typeNameStrategy);
            return this;
        }
        
        public Builder typeDescriptionStrategy(TypeDescriptionStrategy typeDescriptionStrategy) {
            strategies.put(TypeDescriptionStrategy.class, typeDescriptionStrategy);
            return this;
        }
        
        public Builder defaultTypeStrategy(DefaultTypeStrategy defaultTypeStrategy) {
            strategies.put(DefaultTypeStrategy.class, defaultTypeStrategy);
            return this;
        }
        
        public Builder enumValuesStrategy(EnumValuesStrategy enumValuesStrategy) {
            strategies.put(EnumValuesStrategy.class, enumValuesStrategy);
            return this;
        }
        
        public Builder interfacesStrategy(InterfacesStrategy interfacesStrategy) {
            strategies.put(InterfacesStrategy.class, interfacesStrategy);
            return this;
        }
        
        public Builder typeResolverStrategy(TypeResolverStrategy typeResolverStrategy) {
            strategies.put(TypeResolverStrategy.class, typeResolverStrategy);
            return this;
        }
        
        public Builder typeWrapperStrategy(TypeWrapperStrategy typeWrapperStrategy) {
            strategies.put(TypeWrapperStrategy.class, typeWrapperStrategy);
            return this;
        }
        
        public Builder additionalStrategy(Strategy strategy) {
            strategies.put(strategy.getClass(), strategy);
            return this;
        }
        
        public TypeStrategies build() {
            return new TypeStrategies(this);
        }
    }
    
    private TypeStrategies(Builder builder) {
        allStrategies.putAll(builder.strategies);
        this.typeRepository = builder.typeRepository;
    }
}
