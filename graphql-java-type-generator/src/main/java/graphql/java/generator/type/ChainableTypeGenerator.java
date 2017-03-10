package graphql.java.generator.type;

public interface ChainableTypeGenerator extends ITypeGenerator {
    TypeGenerator getNextGen();
    void setNextGen(TypeGenerator nextTypeGen);
}
