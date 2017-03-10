package graphql.java.generator.strategies;

public abstract class ChainableStrategy<S extends Strategy> implements Strategy {
    private S nextStrategy;
    
    public S getNextStrategy() {
        return nextStrategy;
    }
    
    public void setNextStrategy(S nextStrategy) {
        this.nextStrategy = nextStrategy;
    }
}
