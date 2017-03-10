package graphql.java.generator;

/**
 * Any object instances that use this may not be shared among multiple
 * {@link BuildContext}s.
 * @author dwinsor
 *
 */
public class UnsharableBuildContextStorer implements BuildContextAware {
    private BuildContext context;
    
    @Override
    public BuildContext getContext() {
        return context;
    }
    
    @Override
    public void setContext(BuildContext context) {
        if (this.context == null) {
            this.context = context;
        }
    }
}
