package graphql.java.generator.strategies;

import java.util.HashMap;
import java.util.Map;

import graphql.java.generator.BuildContext;
import graphql.java.generator.BuildContextAware;

/**
 * Adds a few percentage points on execution time, but is now more extensible.
 * @author dwinsor
 *
 */
public abstract class AbstractStrategiesContainer implements BuildContextAware {
    protected final Map<Class<? extends Strategy>, Strategy> allStrategies =
            new HashMap<Class<? extends Strategy>, Strategy>();
    
    public Map<Class<? extends Strategy>, Strategy> getAllStrategies() {
        return allStrategies;
    }

    @Override
    public BuildContext getContext() {
        return null;
    }
    
    @Override
    public void setContext(BuildContext context) {
        for (Strategy strategy : getAllStrategies().values()) {
            if (strategy instanceof BuildContextAware) {
                ((BuildContextAware) strategy).setContext(context);
            }
        }
    }
}
